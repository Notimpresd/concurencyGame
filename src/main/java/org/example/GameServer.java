
import java.awt.*;
import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.Border;
public class GameServer {
    private static final int PORT = 5000;
    private static List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private static Queue<String> requestQueue = new LinkedList<>();
    private static ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(0);
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

    public static class ClientHandler implements Runnable {
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        private String clientName;
        private int clientScore = 0;
        private boolean gameEnded = false;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // Assign a unique name based on client count
                clientName = "Player_" + (clients.size());
                out.println("NAME:" + clientName);
                serverLog("Client " + clientName + " connected.");

                // Update GUI with player name and score
                SwingUtilities.invokeLater(() -> GameServerGUI.addPlayerToPanel(clientName, clientScore));

                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    if (inputLine.equals("CLICK") && !gameEnded) {
                        // Add request to queue
                        synchronized (requestQueue) {
                            requestQueue.add(clientName);
                        }
                        serverLog(clientName + " added to queue");

                        // Update GUI with the current queue
                        SwingUtilities.invokeLater(GameServerGUI::updateRequestQueue);
                    }
                }
            } catch (IOException e) {
                serverLog("Client " + clientName + " disconnected unexpectedly.");
            } finally {
                clients.remove(this);
                GameServerGUI.removePlayerFromPanel(clientName);
                try {
                    socket.close();
                } catch (IOException e) {
                    serverLog("Error closing socket for " + clientName);
                }
            }
        }

        public void sendMessage(String message) {
            out.println(message);
        }
    }

    public static class GameServerGUI extends JFrame {
        private static final int WIDTH = 600;
        private static final int HEIGHT = 400;
        private static JPanel playerPanelContainer;
        private static Map<String, JLabel> scoreLabels = new HashMap<>();
        private static JLabel serverIpLabel;
        private static JLabel playersCountLabel;
        private static JList<String> requestList;
        private static DefaultListModel<String> listModel = new DefaultListModel<>();
        private static JLabel timerLabel;
        private static Timer timer;

        public GameServerGUI() {
            setTitle("Game Server");
            setSize(WIDTH, HEIGHT);
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setLayout(new BorderLayout());

            // Panel for IP and player count
            JPanel ipPanel = new JPanel();
            serverIpLabel = new JLabel("Server IP: ");
            playersCountLabel = new JLabel("Players Connected: 0");
            ipPanel.add(serverIpLabel);
            ipPanel.add(playersCountLabel);
            add(ipPanel, BorderLayout.NORTH);

            // Player information panel
            playerPanelContainer = new JPanel();
            playerPanelContainer.setLayout(new GridLayout(0, 3));
            add(new JScrollPane(playerPanelContainer), BorderLayout.CENTER);

            // Request queue panel
            JPanel requestPanel = new JPanel(new BorderLayout());
            requestPanel.setBorder(BorderFactory.createTitledBorder("Concurrency List"));
            requestList = new JList<>(listModel);
            requestPanel.add(new JScrollPane(requestList), BorderLayout.CENTER);
            add(requestPanel, BorderLayout.EAST);

            // Timer label
            timerLabel = new JLabel("Time Left: 5", SwingConstants.CENTER);
            add(timerLabel, BorderLayout.SOUTH);

            // Start timer when a winner is declared
            timer = new Timer(1000, e -> {
                int timeLeft = Integer.parseInt(timerLabel.getText().split(": ")[1]);
                if (timeLeft > 0) {
                    timerLabel.setText("Time Left: " + (timeLeft - 1));
                } else {
                    timer.stop();
                    resetQueue();
                }
            });
        }

        public static void updatePlayersCount(int count) {
            playersCountLabel.setText("Players Connected: " + count);
        }

        public static void addPlayerToPanel(String playerName, int initialScore) {
            JPanel playerPanel = new JPanel();
            playerPanel.setLayout(new BoxLayout(playerPanel, BoxLayout.Y_AXIS));
            playerPanel.setBackground(Color.CYAN);
            playerPanel.setPreferredSize(new Dimension(150, 100));

            JLabel playerLabel = new JLabel("Name: " + playerName);
            playerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel scoreLabel = new JLabel("Score: " + initialScore);
            scoreLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

            playerPanel.add(playerLabel);
            playerPanel.add(scoreLabel);
            playerPanelContainer.add(playerPanel);

            scoreLabels.put(playerName, scoreLabel);

            playerPanelContainer.revalidate();
            playerPanelContainer.repaint();
        }

        public static void removePlayerFromPanel(String playerName) {
            scoreLabels.remove(playerName);
            playerPanelContainer.revalidate();
            playerPanelContainer.repaint();
        }

        public static void updateRequestQueue() {
            listModel.clear();
            synchronized (requestQueue) {
                for (String request : requestQueue) {
                    listModel.addElement(request);
                }
            }
        }

        public static void resetQueue() {
            listModel.clear();
            synchronized (requestQueue) {
                requestQueue.clear();
            }
            timerLabel.setText("Time Left: 5");
        }
    }
}
