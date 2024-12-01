package org.example;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

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
    private RoomListModel roomListModel;
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

        initializeRoomSelectionUI();
        initializeGameUI();
        updateRoomStatuses();
    }


    private void initializeRoomSelectionUI() {
        JPanel roomSelectionPanel = new JPanel(new BorderLayout());

        JLabel titleLabel = new JLabel("Available Rooms", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        roomSelectionPanel.add(titleLabel, BorderLayout.NORTH);

        roomListModel = new RoomListModel();
        JList<Room> roomList = new JList<>(roomListModel);
        roomList.setCellRenderer(new RoomCellRenderer());
        JScrollPane scrollPane = new JScrollPane(roomList);
        roomSelectionPanel.add(scrollPane, BorderLayout.CENTER);

        JButton connectButton = new JButton("Connect");
        connectButton.addActionListener(e -> {
            Room selectedRoom = roomList.getSelectedValue();
            if (selectedRoom != null && selectedRoom.isOnline()) {
                serverIp = selectedRoom.getIp();
                cardLayout.show(mainPanel, "GameUI");
                connectToServer();
            } else {
                JOptionPane.showMessageDialog(this, "Please select an online room to connect!");
            }
        });
        roomSelectionPanel.add(connectButton, BorderLayout.SOUTH);

        mainPanel.add(roomSelectionPanel, "RoomSelection");
        cardLayout.show(mainPanel, "RoomSelection");

        // Mock available rooms
        List<Room> availableRooms = List.of(
                new Room("Room 1", "192.168.0.101"),
                new Room("Room 2", "192.168.0.102"),
                new Room("Room 3", "192.168.0.103"),
                new Room("Room 4", "192.168.0.104"),
                new Room("Room 5", "192.168.0.105"),
                new Room("Room 6", "192.168.0.106"),
                new Room("Room 7", "192.168.0.107"),
                new Room("Room 8", "192.168.0.108")
        );
        availableRooms.forEach(roomListModel::addElement);
    }


    private void initializeGameUI() {
        JPanel gamePanel = new JPanel(new BorderLayout());

        gameButton = new JButton("Wait for Game");
        gameButton.setBackground(Color.RED);
        gameButton.setOpaque(true);
        gameButton.setBorderPainted(false);

        scoreLabel = new JLabel("Score: 0", SwingConstants.CENTER);
        nameLabel = new JLabel("Name: Connecting...", SwingConstants.CENTER);

        JPanel northPanel = new JPanel(new GridLayout(2, 1));
        northPanel.add(scoreLabel);
        northPanel.add(nameLabel);
        gamePanel.add(northPanel, BorderLayout.NORTH);

        gamePanel.add(gameButton, BorderLayout.CENTER);

        JButton backButton = new JButton("Back to Rooms");
        backButton.addActionListener(e -> {
            disconnectFromServer();
            cardLayout.show(mainPanel, "RoomSelection");
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
    }

    private void connectToServer() {
        try {
            clientLog("Attempting to connect to server: " + serverIp + ":" + PORT);
            socket = new Socket(serverIp, PORT);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            clientLog("Connected to server. Local port: " + socket.getLocalPort());
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

    private void updateRoomStatuses() {
        new Thread(() -> {
            while (true) {
                for (int i = 0; i < roomListModel.size(); i++) {
                    Room room = roomListModel.get(i);
                    boolean isOnline = isServerOnline(room.getIp(), PORT);
                    room.setOnline(isOnline);
                }
                SwingUtilities.invokeLater(roomListModel::refresh);
                try {
                    Thread.sleep(5000); // Update every 5 seconds
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }).start();
    }

    private boolean isServerOnline(String ip, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(ip, port), 1000);
            return true;
        } catch (IOException e) {
            return false;
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

class Room {
    private String name;
    private String ip;
    private boolean online;

    public Room(String name, String ip) {
        this.name = name;
        this.ip = ip;
        this.online = false;
    }

    public String getName() {
        return name;
    }

    public String getIp() {
        return ip;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    @Override
    public String toString() {
        return name + " (" + (online ? "Online" : "Offline") + ")";
    }
}

class RoomCellRenderer extends DefaultListCellRenderer {
    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        Component component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (value instanceof Room) {
            Room room = (Room) value;
            setText(room.toString());
            setForeground(room.isOnline() ? Color.GREEN.darker() : Color.RED);
        }
        return component;
    }
}
class RoomListModel extends DefaultListModel<Room> {
    public void refresh() {
        // Notify listeners that the contents have changed
        fireContentsChanged(this, 0, getSize() - 1);
    }
}

