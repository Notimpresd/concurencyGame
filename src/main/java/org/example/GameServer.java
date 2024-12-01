import java.awt.*;
import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.swing.*;
import javax.swing.border.Border;

public class GameServer {
    private static final int PORT = 5000;
    private static List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private static Random random = new Random();
    private static ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int WINNING_SCORE = 5; // Game ends when a player reaches 5 points

    // Centralized logging method
    private static void serverLog(String message) {
        String timestamp = LocalDateTime.now().format(formatter);
        System.out.println("[SERVER " + timestamp + "] " + message);
    }

    public static void main(String[] args) {
        try {
            InetAddress serverAddress = InetAddress.getLocalHost();
            String serverIp = serverAddress.getHostAddress();
            serverLog("Server IP Address: " + serverIp);

            // Update the GUI with the server IP
            SwingUtilities.invokeLater(() -> {
                GameServerGUI serverGUI = new GameServerGUI();
                serverGUI.serverIpLabel.setText("Server IP: " + serverIp);  // Set the server IP label
                serverGUI.setVisible(true);
            });
        } catch (UnknownHostException e) {
            serverLog("Unable to retrieve server IP address.");
        }

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
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

    public static class ClientHandler implements Runnable {
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        private String clientName;
        private String clientIp;
        private int clientScore = 0;
        private boolean gameEnded = false;

        public ClientHandler(Socket socket) {
            this.socket = socket;
            this.clientIp = socket.getInetAddress().getHostAddress();
        }

        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // Assign a unique name based on client count (removed room logic)
                clientName = "Player_" + (clients.size() + 1);
                out.println("NAME:" + clientName);

                // Log client connection details
                serverLog("Client " + clientName + " connected from IP: " + clientIp);

                // Update the GUI with the player name and score
                GameServerGUI.addPlayerToPanel(clientName, clientScore);

                // Increment the player count
                SwingUtilities.invokeLater(() -> GameServerGUI.updatePlayersCount(clients.size()));

                // Initial game state
                if (!gameEnded) {
                    sendMessage("GREEN");
                }

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
                serverLog("Client " + clientName + " disconnected.");
                clients.remove(this);

                // Remove player from the GUI
                GameServerGUI.removePlayerFromPanel(clientName);

                // Decrement the player count
                SwingUtilities.invokeLater(() -> GameServerGUI.updatePlayersCount(clients.size()));

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

            // Update the winner score
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
        }
    }

    public static class GameServerGUI extends JFrame {
        private static final int WIDTH = 600;
        private static final int HEIGHT = 300;
        private static JPanel playerPanelContainer;
        private static Map<String, JPanel> playerPanels = new HashMap<>();
        private static Map<String, JLabel> scoreLabels = new HashMap<>();
        public static JLabel serverIpLabel;  // Declare a JLabel to display the server IP
        public static JLabel playersCountLabel;  // Declare a JLabel to display the number of players

        public GameServerGUI() {
            setTitle("Game Server");
            setSize(WIDTH, HEIGHT);
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setLayout(new BorderLayout());
            setAlwaysOnTop(true);

            // Add a panel for the server IP label and player count label
            JPanel ipPanel = new JPanel();
            serverIpLabel = new JLabel("Server IP: ");
            playersCountLabel = new JLabel("     Players Connected: 0");
            ipPanel.add(serverIpLabel);
            ipPanel.add(playersCountLabel);
            add(ipPanel, BorderLayout.NORTH);

            playerPanelContainer = new JPanel();
            playerPanelContainer.setLayout(new GridLayout(0, 3));  // Three columns for player panels
            add(new JScrollPane(playerPanelContainer), BorderLayout.CENTER);
        }

        // Method to update the number of players connected
        public static void updatePlayersCount(int count) {
            playersCountLabel.setText("Players Connected: " + count);
        }

        public static void addPlayerToPanel(String playerName, int initialScore) {
            JPanel playerPanel = new JPanel();
            playerPanel.setLayout(new BoxLayout(playerPanel, BoxLayout.Y_AXIS));
            playerPanel.setBorder(new RoundedBorder(15));  // Rounded corners
            playerPanel.setBackground(Color.CYAN);
            playerPanel.setPreferredSize(new Dimension(150, 100));  // Size for each player panel

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
