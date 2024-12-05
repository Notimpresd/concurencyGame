
---

### **README for Multiplayer Reaction Game**

---

## **Project Overview**
This project is a simple multiplayer reaction-based game implemented in Java. It consists of a **server** and multiple **clients** connected via sockets, with a graphical user interface (GUI) for both the server and clients.

The goal of the game is to click a button as quickly as possible when the server sends a "GO" signal. The player who reacts the fastest earns a point. The game continues until one player reaches the winning score.

---

## **Features**
- **Multiplayer Support:** Multiple clients can connect to the server simultaneously.
- **Real-time Gameplay:** Server broadcasts signals to all clients, and players respond based on the signal.
- **Dynamic GUI:**
  - **Server:** Displays connected players, their scores, and updates dynamically.
  - **Client:** Shows the player's name, current score, and a clickable button for gameplay.
- **Concurrency:** Server manages clients using multithreading.
- **Customizable Gameplay:**
  - Adjustable winning score.
  - Adjustable signal intervals.
  
**Note:** Currently, both the **server IP** (in the client) and **player addresses** are **hardcoded**. This means the server IP must be manually set in the client code, and the client doesn't support dynamic discovery or address configuration.

---

## **Technologies Used**
- **Language:** Java
- **Networking:** Sockets (`ServerSocket` and `Socket`)
- **Concurrency:** Threads (`Thread`, `ExecutorService`)
- **GUI:** Swing library for graphical interfaces

---

## **Setup Instructions**

### **Prerequisites**
1. Java Development Kit (JDK) 8 or higher installed.
2. An IDE (e.g., IntelliJ IDEA, Eclipse) or a terminal for running Java applications.

### **Steps to Run**
1. **Clone the Repository:**
   ```bash
   git clone <repository-url>
   cd <repository-folder>
   ```

2. **Compile the Project:**
   If using the terminal:
   ```bash
   javac -d bin src/**/*.java
   ```

3. **Start the Server:**
   ```bash
   java -cp bin GameServer
   ```
   The server will start and wait for clients to connect.

4. **Start Clients:**
   Run the client application for each player:
   ```bash
   java -cp bin GameClient
   ```
   **Important:** The **server IP** is **hardcoded** in the client (`serverIp = "10.31.5.52"`). Change this in the `GameClient` class if you are running the server on a different machine.

5. **Play the Game:**
   - The server will periodically send a "GO" signal.
   - Players must click the button on their GUI when the signal is green.
   - The fastest player to click wins the round.

---

## **Game Rules**
1. Players must wait for the green signal to click the button.
2. Clicking before the green signal will result in a penalty (depending on server logic).
3. The first player to reach the defined winning score wins the game.

---

## **Project Structure**
```
src/
├── GameServer.java         # Main server logic
├── GameServerGUI.java      # Server-side GUI
├── ClientHandler.java      # Thread to handle each client
├── GameClient.java         # Main client logic
└── GameClientGUI.java      # Client-side GUI
```

---

## **Future Enhancements**
- Add encryption for secure communication.
- Implement player authentication (e.g., username/password).
- Create a leaderboard system.
- Add a settings menu for server configurations (e.g., score limits, interval time).
- Optimize server performance for handling a large number of clients.
- Allow dynamic IP address configuration instead of hardcoded addresses.

---

## **Contributing**
1. Fork the repository.
2. Create a new branch for your feature:
   ```bash
   git checkout -b feature-name
   ```
3. Commit your changes:
   ```bash
   git commit -m "Add your message here"
   ```
4. Push to your branch:
   ```bash
   git push origin feature-name
   ```
5. Open a pull request.

---

## **License**
This project is licensed under the MIT License. See the `LICENSE` file for more details.

---
