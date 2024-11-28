**Resource Collector Game - Multiplayer**
**Overview**

A simple multiplayer game where players collect resources to win. The first player to collect 10 resources wins. The game uses a server to handle multiple players, with deadlock prevention to ensure smooth gameplay.

**Features**
Multiplayer: Multiple players can connect to the server.
Concurrency: Handles multiple players collecting resources at the same time.
Deadlock Prevention: Prevents players from getting stuck while collecting resources.
**How to Run
**1. Start the Server
Run GameServer.java in IntelliJ. The server will listen on port 12345.
2. Start the Clients
Run GameClient.java in IntelliJ to open a client window.
You can start multiple client windows for more players.
3. Play the Game
Click the "Collect Resource" button to gather resources.
Collect 10 resources to win.
Deadlock Prevention
The server uses locks with timeouts to prevent players from being stuck if they try to collect resources at the same time.
**Project Structure
**
- GameServer.java    // Server that handles connections and game logic.
- GameClient.java    // Client that players use to collect resources.
  
**License
**Free to use for educational purposes.

