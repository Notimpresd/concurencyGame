import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;

public class GameClient extends JFrame {
    private JButton gameButton;
    private JLabel scoreLabel;
    private JLabel nameLabel;
    private int score = 0;
    private boolean canClick = false;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    public GameClient() {
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
            socket = new Socket("localhost", 5000);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // Start listening for server messages
            new Thread(this::listenForServerMessages).start();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Could not connect to server");
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
                } else if (inputLine.equals("GREEN")) {
                    SwingUtilities.invokeLater(() -> {
                        gameButton.setBackground(Color.GREEN);
                        gameButton.setText("CLICK!");
                        canClick = true;
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
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GameClient client = new GameClient();
            client.setVisible(true);
        });
    }
}