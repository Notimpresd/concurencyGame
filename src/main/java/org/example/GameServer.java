import java.awt.*;
import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import javax.swing.*;
import javax.swing.border.Border;

public class GameServer {
    private static final int PORT = 5000;
    private static List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private static Random random = new Random();
    private static ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static Semaphore clientSemaphore = new Semaphore(1);  // Semaphore to manage concurrent access to clients list
    private static final int WINNING_SCORE = 5; // Game ends when a player reaches 5 points

    // Centralized logging method
    private static void serverLog(String message) {
        String timestamp = LocalDateTime.now().format(formatter);
        System.out.println("[SERVER " + timestamp + "] " + message);
    }

    public static void main(String[] args) {
        // Print the server's local IP address
        try {
            InetAddress serverAddress = InetAddress.getLocalHost();
            String serverIp = serverAddress.getHostAddress();
            serverLog("Server IP Address: " + serverIp);
        } catch (UnknownHostException e) {
            serverLog("Unable to retrieve server IP address.");
        }

        SwingUtilities.invokeLater(() -> {
            GameServerGUI serverGUI = new GameServerGUI();
            serverGUI.setVisible(true);
        });

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
        private JPanel playerPanel;  // Panel for GUI display
        private boolean gameEnded = false;  // Flag to check if the game has ended

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

                // Update the GUI
                GameServerGUI.addPlayerToPanel(clientName, clientScore);

                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    if (inputLine.equals("CLICK") && !gameEnded) {
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

                // Remove player from the GUI
                GameServerGUI.removePlayerFromPanel(clientName);
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

            // Update the winner score using semaphore to prevent concurrent modification
            try {
                clientSemaphore.acquire();
                if (winner.equals(clientName)) {
                    clientScore++;
                    GameServerGUI.updatePlayerScore(clientName, clientScore);

                    // Check if the game ends
                    if (clientScore >= WINNING_SCORE && !gameEnded) {
                        gameEnded = true;
                        serverLog(clientName + " wins the game!");
                        for (ClientHandler clientHandler : clients) {
                            clientHandler.sendMessage("GAME_OVER:" + clientName + " wins!");
                        }
                    }
                }
            } catch (InterruptedException e) {
                serverLog("Error updating score: " + e.getMessage());
            } finally {
                clientSemaphore.release();
            }
        }
    }

    public static class GameServerGUI extends JFrame {
        private static final int WIDTH = 600;
        private static final int HEIGHT = 400;
        private static JPanel playerPanelContainer;
        private static Map<String, JPanel> playerPanels = new HashMap<>();
        private static Map<String, JLabel> scoreLabels = new HashMap<>();

        public GameServerGUI() {
            setTitle("Game Server");
            setSize(WIDTH, HEIGHT);
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setLayout(new BorderLayout());

            playerPanelContainer = new JPanel();
            playerPanelContainer.setLayout(new GridLayout(0, 3));  // Three columns for player panels
            add(new JScrollPane(playerPanelContainer), BorderLayout.CENTER);
        }

        public static void addPlayerToPanel(String playerName, int initialScore) {
            JPanel playerPanel = new JPanel();
            playerPanel.setLayout(new BoxLayout(playerPanel, BoxLayout.Y_AXIS));
            playerPanel.setBorder(new RoundedBorder(15));  // Rounded corners
            playerPanel.setBackground(Color.CYAN);
            playerPanel.setPreferredSize(new Dimension(150, 150));  // Size for each player panel

            JLabel playerLabel = new JLabel(playerName);
            playerLabel.setFont(new Font("Arial", Font.BOLD, 14));
            playerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel scoreLabel = new JLabel("Score: " + initialScore);
            scoreLabel.setFont(new Font("Arial", Font.PLAIN, 12));
            scoreLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

            playerPanel.add(playerLabel);
            playerPanel.add(scoreLabel);
            playerPanelContainer.add(playerPanel);

            playerPanels.put(playerName, playerPanel);
            scoreLabels.put(playerName, scoreLabel);

            playerPanelContainer.revalidate();
            playerPanelContainer.repaint();
        }

        public static void updatePlayerScore(String playerName, int newScore) {
            JLabel scoreLabel = scoreLabels.get(playerName);
            if (scoreLabel != null) {
                scoreLabel.setText("Score: " + newScore);
            }
        }

        public static void removePlayerFromPanel(String playerName) {
            JPanel playerPanel = playerPanels.get(playerName);
            if (playerPanel != null) {
                playerPanelContainer.remove(playerPanel);
                playerPanels.remove(playerName);
                scoreLabels.remove(playerName);
            }
            playerPanelContainer.revalidate();
            playerPanelContainer.repaint();
        }
    }

    // Custom border class for rounded corners
    public static class RoundedBorder implements Border {
        private int radius;

        public RoundedBorder(int radius) {
            this.radius = radius;
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(radius, radius, radius, radius);
        }

        @Override
        public boolean isBorderOpaque() {
            return true;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            g.setColor(Color.lightGray);
            g.fillRoundRect(x, y, width - 1, height - 1, radius, radius);
        }
    }
}
