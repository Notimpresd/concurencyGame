
import javax.swing.*;
        import java.awt.*;
        import java.io.*;
        import java.net.*;
        import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class GameClient extends JFrame {
    private JButton gameButton;
    private JLabel scoreLabel;
    private JLabel nameLabel;
    private int score = 0;
    private boolean canClick = false;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String serverIp;
    private static final int PORT = 5000;
    private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Centralized logging method for client
    private void clientLog(String message) {
        String timestamp = LocalDateTime.now().format(formatter);
        System.out.println("[CLIENT " + timestamp + "] " + message);
    }

    public GameClient(String serverIp) {
        this.serverIp = serverIp;
        setTitle("Reaction Game");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Initialize components
        gameButton = new JButton("Wait for Game");
        gameButton.setBackground(Color.RED);
        gameButton.setOpaque(true);
        gameButton.setBorderPainted(false);

        scoreLabel = new JLabel("Score: 0");
        nameLabel = new JLabel("Name: Connecting...");

        // Add components
        add(gameButton, BorderLayout.CENTER);
        add(scoreLabel, BorderLayout.NORTH);
        add(nameLabel, BorderLayout.SOUTH);

        // Button click listener
        gameButton.addActionListener(e -> {
            if (canClick) {
                clientLog("Button clicked!");
                out.println("CLICK");
                canClick = false;
                gameButton.setBackground(Color.RED);
            }
        });

        // Connect to server
        connectToServer();
    }

    private void connectToServer() {
        try {
            clientLog("Attempting to connect to server: " + serverIp + ":" + PORT);
            socket = new Socket(serverIp, PORT);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // Log connection details
            clientLog("Connected to server. Local port: " + socket.getLocalPort());

            // Start listening for server messages
            new Thread(this::listenForServerMessages).start();
        } catch (IOException e) {
            clientLog("Connection failed: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Could not connect to server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void listenForServerMessages() {
        try {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                if (inputLine.startsWith("NAME:")) {
                    String name = inputLine.substring(5);
                    nameLabel.setText("Name: " + name);
                    clientLog("Received name from server: " + name);
                } else if (inputLine.equals("GREEN")) {
                    clientLog("GREEN signal received!");
                    SwingUtilities.invokeLater(() -> {
                        gameButton.setBackground(Color.GREEN);
                        gameButton.setText("CLICK!");
                        canClick = true;
                    });
                } else if (inputLine.startsWith("WINNER:")) {
                    String winner = inputLine.substring(7);
                    clientLog("Winner announced: " + winner);
                    SwingUtilities.invokeLater(() -> {
                        gameButton.setBackground(Color.RED);
                        gameButton.setText("Wait for Game");
                        canClick = false;

                        if (winner.equals(nameLabel.getText().substring(6))) {
                            score++;
                            scoreLabel.setText("Score: " + score);
                            clientLog("You won a point! Total score: " + score);
                        }
                    });
                }
            }
        } catch (IOException e) {
            clientLog("Server connection lost: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        // Allow specifying server IP via command line or use default
        String serverIp = args.length > 0 ? args[0] : "localhost";

        SwingUtilities.invokeLater(() -> {
            GameClient client = new GameClient(serverIp);
            client.setVisible(true);
        });
    }
}