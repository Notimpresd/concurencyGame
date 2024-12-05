package org.example;

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
    private JLabel localIpLabel;
    private JLabel serverIpLabel;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String serverIp = "10.31.5.52"; // Set a predefined server IP
    private static final int PORT = 5000;
    private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private CardLayout cardLayout;
    private JPanel mainPanel;

    public GameClient() {
        setTitle("Reaction Game");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);
        add(mainPanel, BorderLayout.CENTER);

        initializeGameUI();
    }

    private void initializeGameUI() {
        JPanel gamePanel = new JPanel(new BorderLayout());
        setAlwaysOnTop(true);

        gameButton = new JButton("Wait for Game");
        gameButton.setBackground(Color.RED);
        gameButton.setOpaque(true);
        gameButton.setBorderPainted(false);

        scoreLabel = new JLabel("Score: 0", SwingConstants.CENTER);
        nameLabel = new JLabel("Name: Connecting...", SwingConstants.CENTER);

        localIpLabel = new JLabel("Local IP: Connecting...", SwingConstants.CENTER);
        serverIpLabel = new JLabel("Server IP: Connecting...", SwingConstants.CENTER);

        JPanel northPanel = new JPanel(new GridLayout(4, 1));
        northPanel.add(scoreLabel);
        northPanel.add(nameLabel);
        northPanel.add(localIpLabel);
        northPanel.add(serverIpLabel);
        gamePanel.add(northPanel, BorderLayout.NORTH);

        gamePanel.add(gameButton, BorderLayout.CENTER);

        JButton backButton = new JButton("Disconnect");
        backButton.addActionListener(e -> {
            disconnectFromServer();
            JOptionPane.showMessageDialog(this, "Disconnected from server.");
        });
        gamePanel.add(backButton, BorderLayout.SOUTH);

        gameButton.addActionListener(e -> {
            if (canClick) {
                clientLog("Button clicked!");
                out.println("CLICK");
                canClick = false;
                gameButton.setBackground(Color.RED);
            }
        });

        mainPanel.add(gamePanel, "GameUI");
        cardLayout.show(mainPanel, "GameUI");

        // Connect directly to the predefined server
        connectToServer();
    }

    private void connectToServer() {
        try {
            clientLog("Attempting to connect to server: " + serverIp + ":" + PORT);
            socket = new Socket(serverIp, PORT);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // Update labels in GUI
            String localIp = socket.getLocalAddress().getHostAddress();
            SwingUtilities.invokeLater(() -> {
                localIpLabel.setText("Local IP: " + localIp);
                serverIpLabel.setText("Server IP: " + serverIp);
            });

            clientLog("Connected to server " + serverIp + ":" + PORT);
            clientLog("Client local IP address: " + localIp);

            new Thread(this::listenForServerMessages).start();
        } catch (IOException e) {
            clientLog("Connection failed: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Could not connect to server: " + e.getMessage());
        }
    }
    private void disconnectFromServer() {
        try {
            if (socket != null) {
                socket.close();
                clientLog("Disconnected from server.");
            }
        } catch (IOException e) {
            clientLog("Error while disconnecting: " + e.getMessage());
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
                    SwingUtilities.invokeLater(() -> {
                        gameButton.setBackground(Color.GREEN);
                        gameButton.setText("CLICK!");
                        canClick = true;
                    });
                } else if (inputLine.equals("RED")) {
                    SwingUtilities.invokeLater(() -> {
                        gameButton.setBackground(Color.RED);
                        gameButton.setText("Wait for Game");
                        canClick = false;
                    });
                } else if (inputLine.startsWith("WINNER:")) {
                    String winner = inputLine.substring(7);
                    SwingUtilities.invokeLater(() -> {
                        gameButton.setBackground(Color.RED);
                        gameButton.setText("Wait for Game");
                        canClick = false;

                        if (winner.equals(nameLabel.getText().substring(6))) {
                            score++;
                            scoreLabel.setText("Score: " + score);
                        }
                    });
                }
            }
        } catch (IOException e) {
            clientLog("Server connection lost: " + e.getMessage());
        }
    }

    private void clientLog(String message) {
        String timestamp = LocalDateTime.now().format(formatter);
        System.out.println("[CLIENT " + timestamp + "] " + message);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GameClient client = new GameClient();
            client.setVisible(true);
        });
    }
}
