
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class GameServer {
    private static final int PORT = 5000;
    private static List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private static Random random = new Random();
    private static ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public static void main(String[] args) {
        try {
            ServerSocket serverSocket = new ServerSocket(PORT);
            System.out.println("Game Server started on port " + PORT);

            // Start the game signal scheduler
            startGameSignals();

            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clients.add(clientHandler);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
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
                    e.printStackTrace();
                }

                // Send green signal to all clients
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

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // Generate random name for client
                clientName = "Player_" + UUID.randomUUID().toString().substring(0, 5);
                out.println("NAME:" + clientName);

                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    if (inputLine.equals("CLICK")) {
                        // Broadcast the winner
                        broadcastWinner(clientName);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                clients.remove(this);
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public void sendMessage(String message) {
            out.println(message);
        }

        private void broadcastWinner(String winner) {
            for (ClientHandler client : clients) {
                client.sendMessage("WINNER:" + winner);
            }
        }
    }
}