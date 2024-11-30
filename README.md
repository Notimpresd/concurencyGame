

# Multiplayer Game with Concurrency Management

This is a simple multiplayer game implemented in Java where multiple players can join, compete, and see their scores. The game uses server-client architecture, and concurrency issues such as race conditions and deadlocks are managed using semaphores to ensure proper synchronization.

## Features

- **Multiplayer**: Multiple clients can connect to the server and participate in the game simultaneously.
- **Concurrency Handling**: Uses semaphores to ensure thread-safe updates to shared resources such as player scores.
- **Game State Synchronization**: Ensures that all players have a consistent view of the game state.
- **Client-Server Architecture**: The server manages the game logic, player connections, and broadcasting of game results to clients.

## Prerequisites

- Java Development Kit (JDK) 8 or higher
- A text editor or Integrated Development Environment (IDE) like IntelliJ IDEA
- Basic understanding of Java and multithreading concepts

## Installation

1. Clone this repository or download the source code files.
   
   ```bash
   git clone https://github.com/yourusername/multiplayer-game.git
   ```

2. Open the project in your IDE (e.g., IntelliJ IDEA).
   
3. Compile the project:
   
   - In IntelliJ, click on `Build` > `Build Project` or use the shortcut `Ctrl+F9` (Windows/Linux) or `Cmd+F9` (macOS).

## Running the Game

1. **Start the Server**:
   - Run the `GameServer.java` file. The server will start listening for client connections.
   
   ```bash
   javac GameServer.java
   java GameServer
   ```

2. **Start a Client**:
   - Run the `GameClient.java` file for each player to connect to the server and participate in the game.
   
   ```bash
   javac GameClient.java
   java GameClient
   ```

3. **Join the Game**:
   - Players will be prompted to enter their name.
   - The game begins once all players are connected and ready.

## How It Works

### Server-Side

- The `GameServer` listens for incoming connections from clients.
- When a client connects, a new `ClientHandler` thread is spawned to handle the communication.
- The server broadcasts game results (e.g., the winner) to all clients.
- **Concurrency**: The server uses a semaphore to ensure that player scores and the game state are updated safely. When a player wins, their score is updated in a thread-safe manner by acquiring the semaphore.

### Client-Side

- The `GameClient` connects to the server and sends player input (e.g., button presses).
- The client listens for updates from the server (such as the winner or the end of the game).
- **UI**: A simple graphical interface is provided to interact with the game.

### Concurrency and Synchronization

- **Semaphore Usage**: A semaphore is used to ensure that only one thread can access and modify the game state at a time, preventing race conditions and deadlocks.
- **Thread Safety**: The game state (scores, player names, etc.) is protected from concurrent modification, ensuring consistency for all players.

## Concurrency Issues Solved

1. **Race Conditions**: Without synchronization, two players could update their scores simultaneously, causing inconsistent results. Semaphores ensure that only one player’s score is updated at a time.
   
2. **Deadlocks**: Using semaphores and carefully managing thread access to shared resources, the game avoids situations where threads are blocked waiting on each other indefinitely.

3. **Consistency**: All players see a consistent view of the game state, and the server broadcasts the same information to all players.

## Example Game Flow

1. Player 1 connects to the server and enters their name.
2. Player 2 joins the game.
3. The server begins the game, and players take turns pressing a button to compete.
4. When a player wins, their score is updated safely using semaphores.
5. The server broadcasts the winner to all clients.
6. The game ends when a player reaches the winning score, and the game state is synchronized across all clients.

## Code Structure

- `GameServer.java`: Handles the server-side logic, manages client connections, and synchronizes access to shared resources using semaphores.
- `GameClient.java`: Manages the client-side logic, including the user interface and communication with the server.
- `ClientHandler.java`: Manages individual client connections and player actions.
- `GameServerGUI.java`: Provides a simple graphical interface for the server.
- `GameClientGUI.java`: Provides a graphical interface for the client.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
