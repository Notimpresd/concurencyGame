import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

public class GameServer {
    private static final int PORT = 5000;
    private static List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private static Random random = new Random();
    private static ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Centralized logging method
    private static void serverLog(String message) {
        String timestamp = LocalDateTime.now().format(formatter);
        System.out.println("[SERVER " + timestamp + "] " + message);
    }

    public static void main(String[] args) {
        try {
            ServerSocket serverSocket = new ServerSocket(PORT);
            serverLog("Game Server started on port " + PORT);

            // Start the game signal scheduler
            startGameSignals();

            while (true) {
                Socket clientSocket = serverSocket.accept();
                String clientIp = clientSocket.getInetAddress().getHostAddress();
                serverLog("New client connected from IP: " + clientIp);

                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clients.add(clientHandler);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            serverLog("Server error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void startGameSignals() {
        scheduler.scheduleAtFixedRate(() -> {
            if (!clients.isEmpty()) {
                // Delay between 3-7 seconds
                int delay = random.nextInt(4000) + 3000;
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    serverLog("Game signal interruption: " + e.getMessage());
                }

                // Send green signal to all clients
                serverLog("Sending GREEN signal to all clients");
                for (ClientHandler client : clients) {
                    client.sendMessage("GREEN");
                }
            }
        }, 5, 10, TimeUnit.SECONDS);
    }

    private static class ClientHandler implements Runnable {
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        private String clientName;
        private String clientIp;
        private int clientScore = 0;

        public ClientHandler(Socket socket) {
            this.socket = socket;
            this.clientIp = socket.getInetAddress().getHostAddress();
        }

        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // Generate random name for client
                clientName = "Player_" + UUID.randomUUID().toString().substring(0, 5);
                out.println("NAME:" + clientName);

                // Log client connection details
                serverLog("Client " + clientName + " connected from IP: " + clientIp);

                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    if (inputLine.equals("CLICK")) {
                        // Log client click
                        serverLog("Client " + clientName + " clicked the button");

                        // Broadcast the winner
                        broadcastWinner(clientName);
                    }
                }
            } catch (IOException e) {
                serverLog("Client " + clientName + " disconnected unexpectedly: " + e.getMessage());
            } finally {
                // Log client disconnection
                serverLog("Client " + clientName + " disconnected. Total players: " + (clients.size() - 1));
                clients.remove(this);
                try {
                    socket.close();
                } catch (IOException e) {
                    serverLog("Error closing socket for " + clientName + ": " + e.getMessage());
                }
            }
        }

        public void sendMessage(String message) {
            out.println(message);
        }

        private void broadcastWinner(String winner) {
            serverLog("Declaring winner: " + winner);
            for (ClientHandler client : clients) {
                client.sendMessage("WINNER:" + winner);
            }
        }
    }
}