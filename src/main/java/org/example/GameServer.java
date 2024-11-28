import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

public class GameServer {
    private static final int WIN_CONDITION = 10;
    private final ConcurrentHashMap<Socket, Integer> playerResources = new ConcurrentHashMap<>();
    private final Lock resourceLock1 = new ReentrantLock();
    private final Lock resourceLock2 = new ReentrantLock();

    public static void main(String[] args) {
        new GameServer().startServer();
    }

    public void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(12345)) {
            System.out.println("Server started on port 12345");
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class ClientHandler implements Runnable {
        private final Socket clientSocket;
        private PrintWriter out;
        private BufferedReader in;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                out = new PrintWriter(clientSocket.getOutputStream(), true);

                playerResources.put(clientSocket, 0); // Start with 0 resources

                String message;
                while ((message = in.readLine()) != null) {
                    if (message.equals("COLLECT_RESOURCE")) {
                        collectResource(clientSocket);
                    } else if (message.equals("QUIT")) {
                        break;
                    }
                }

                // Clean up when the client disconnects
                playerResources.remove(clientSocket);
                clientSocket.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void collectResource(Socket playerSocket) {
            try {
                // Try to acquire both locks, if both are acquired within a timeout, proceed
                boolean resource1Locked = resourceLock1.tryLock(1, TimeUnit.SECONDS);
                boolean resource2Locked = resourceLock2.tryLock(1, TimeUnit.SECONDS);

                if (resource1Locked && resource2Locked) {
                    // Both resources acquired successfully
                    int resources = playerResources.get(playerSocket) + 1;
                    playerResources.put(playerSocket, resources);
                    out.println("You collected a resource! Total resources: " + resources);

                    if (resources >= WIN_CONDITION) {
                        out.println("You won the game with " + WIN_CONDITION + " resources!");
                    }
                } else {
                    // If unable to acquire both locks, release any acquired lock and retry
                    if (resource1Locked) resourceLock1.unlock();
                    if (resource2Locked) resourceLock2.unlock();

                    out.println("Unable to collect resources due to a timeout. Try again.");
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
