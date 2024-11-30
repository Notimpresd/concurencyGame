import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class GameClient {
    private static final String SERVER_ADDRESS = "127.0.0.1"; // Replace with your server's public IP for remote play
    private static final int PORT = 12345;

    private int score = 0;
    private boolean buttonEnabled = false;
    private final Object lock = new Object();

    public static void main(String[] args) {
        new GameClient().startClient();
    }

    public void startClient() {
        JFrame frame = new JFrame("Game Client");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(300, 200);

        JLabel scoreLabel = new JLabel("Score: 0", SwingConstants.CENTER);
        JButton button = new JButton("Press Me");
        button.setEnabled(false);
        button.setBackground(Color.RED);

        button.addActionListener(e -> {
            synchronized (lock) {
                if (buttonEnabled) {
                    score++;
                    scoreLabel.setText("Score: " + score);
                    buttonEnabled = false;
                    button.setBackground(Color.RED);
                    button.setEnabled(false);
                    sendToServer("PRESSED");
                }
            }
        });

        frame.setLayout(new BorderLayout());
        frame.add(scoreLabel, BorderLayout.NORTH);
        frame.add(button, BorderLayout.CENTER);
        frame.setVisible(true);

        try (Socket socket = new Socket(SERVER_ADDRESS, PORT);
             var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             var out = new PrintWriter(socket.getOutputStream(), true)) {

            while (true) {
                String response = in.readLine();
                if ("GREEN".equals(response)) {
                    synchronized (lock) {
                        buttonEnabled = true;
                        button.setBackground(Color.GREEN);
                        button.setEnabled(true);
                    }
                } else if ("RED".equals(response)) {
                    synchronized (lock) {
                        buttonEnabled = false;
                        button.setBackground(Color.RED);
                        button.setEnabled(false);
                    }
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(frame, "Disconnected from server: " + e.getMessage());
            System.exit(1);
        }
    }

    private void sendToServer(String message) {
        // Notify the server that the client pressed the button
        System.out.println("Message to server: " + message);
    }
}
