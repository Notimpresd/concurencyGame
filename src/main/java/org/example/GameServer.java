import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

public class GameServer {
    public static final int WIN_CONDITION = 10;
    protected final ConcurrentHashMap<Socket, Integer> playerResources = new ConcurrentHashMap<>();
    public final Lock resourceLock1;

    {
        resourceLock1 = new ReentrantLock();
    }

    private final Lock resourceLock2 = new ReentrantLock();

    public static void main(String[] args) {
        new GameServer().startServer();
    }

    public void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(1234)) {
            System.out.println("Server started on port 1234");
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress().getHostAddress());
                // Initialize player's resources and print the initial resource count
                playerResources.put(clientSocket, 0);
                System.out.println("Player " + clientSocket.getInetAddress().getHostAddress() + " has 0 resources.");
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

                String message;
                while ((message = in.readLine()) != null) {
                    if (message.equals("COLLECT_RESOURCE")) {
                        collectResource(clientSocket);
                    } else if (message.equals("QUIT")) {
                        handleQuit(clientSocket);
                        break;  // Exit the loop when the player quits
                    }
                }
            } catch (IOException e) {
                System.out.println("Client disconnected or error occurred: " + e.getMessage());
            } finally {
                cleanupClientResources();
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
                    System.out.println("Player " + playerSocket.getInetAddress().getHostAddress() + " has " + resources + " resources.");

                    if (resources >= WIN_CONDITION) {
                        out.println("You won the game with " + WIN_CONDITION + " resources!");
                        System.out.println("Player " + playerSocket.getInetAddress().getHostAddress() + " has won the game with " + resources + " resources.");
                        cleanupClientResources();  // Close the client connection after winning
                        return; // Stop further game interaction
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

        private void handleQuit(Socket playerSocket) {
            // Handle player quit action
            int finalResources = playerResources.get(playerSocket);
            System.out.println("Player " + playerSocket.getInetAddress().getHostAddress() + " has quit the game with " + finalResources + " resources.");
            cleanupClientResources();
        }

        private void cleanupClientResources() {
            try {
                // Clean up when the client disconnects or quits
                playerResources.remove(clientSocket);
                if (clientSocket != null && !clientSocket.isClosed()) {
                    clientSocket.close();  // Close the client socket
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}