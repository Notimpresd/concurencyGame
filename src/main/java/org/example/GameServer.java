import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class GameServer {
    private static final int PORT = 12345;
    private boolean buttonActive = false;  // This keeps track of the button's state
    private final Object lock = new Object();  // Synchronization lock for button state

    public static void main(String[] args) throws IOException {
        new GameServer().startServer();
    }

    public void startServer() throws IOException {
        System.out.println("Server started on port " + PORT);
        ServerSocket serverSocket = new ServerSocket(PORT);
        var pool = Executors.newCachedThreadPool();

        while (true) {
            Socket clientSocket = serverSocket.accept();
            pool.execute(() -> handleClient(clientSocket));
        }
    }

    private void handleClient(Socket clientSocket) {
        try (var out = new PrintWriter(clientSocket.getOutputStream(), true)) {
            while (true) {
                // Wait for at least 3 seconds before turning the button green
                TimeUnit.SECONDS.sleep(3);

                synchronized (lock) {
                    if (!buttonActive) {
                        buttonActive = true;
                        out.println("GREEN");  // Signal to the client to turn the button green
                        System.out.println("Server: Button turned GREEN (after 3 seconds)");
                    }
                }

                // Wait for a client to press the button and then turn it red
                TimeUnit.MILLISECONDS.sleep(100);  // Adjust this time as needed for the next cycle
            }
        } catch (Exception e) {
            System.out.println("Client disconnected: " + e.getMessage());
        }
    }

    public void pressButton() {
        synchronized (lock) {
            buttonActive = false;
            System.out.println("Server: Button pressed, turning RED...");
            // Notify all clients to turn the button red
            // (This can be done via broadcasting to all connected clients)
        }
    }
}
