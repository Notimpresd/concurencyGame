import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;

public class GameClient {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private JLabel statusLabel;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new GameClient().startClient());
    }

    public void startClient() {
        try {
            socket = new Socket("localhost", 1234);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            createAndShowGUI();
            listenForServerMessages();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createAndShowGUI() {
        JFrame frame = new JFrame("Resource Collector Game");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(300, 200);

        JButton collectButton = new JButton("Collect Resource");
        collectButton.addActionListener(e -> collectResource());

        statusLabel = new JLabel("Collect resources to win!", SwingConstants.CENTER);

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(collectButton, BorderLayout.CENTER);
        panel.add(statusLabel, BorderLayout.SOUTH);

        frame.add(panel);
        frame.setVisible(true);
    }

    private void listenForServerMessages() {
        new Thread(() -> {
            try {
                String message;
                while ((message = in.readLine()) != null) {
                    statusLabel.setText(message);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void collectResource() {
        if (out != null) {
            out.println("COLLECT_RESOURCE");
        }
    }
}
