#### Software Architecture and Platforms - a.y. 2025-2026

## Tic-Tac-Toe Game Server   

v1.1.0-20251007

It is a simpl client-server  system implementing some raw functionalities of a Tic-Tac-Toe (TTT) game server. 
 - The system allows for creating and playing multiple independent TTT games. In order to play in games, users must first register to the server.
 - Once registered, a user can join and play any ongoing game. 

The system is composed of a backend and a frontend

- **Backend**
  - Written in Java, using Vert.x event-loop-based framework. It exposes an HTTP endpoint providing an API to register users, create new games,  join and play a game

    - To run it from the command line:

      `mvn compile exec:java -Dexec.mainClass="ttt_backend.TTTBackend"`   

      (to be run from the package root, where `pom.xml` is located)
  
  - Notes about the design and implementation: 
    - `ttt_backend.TTTBackend` is a big monolitic backend implemented using Vert.x + Web reactive framework. The backend receives HTTP requests (port `8080`) and processes them. The requests can be: 
      - **to register a new user** to the game server, given its user name. 
        - It generates a unique user id. An hash map `users` is used to keep track of the set of registered users (class (`domain.User`). 
        - A simple JSON dbase (`users.json` file) is used to persist the set of registered users.  
      - **to create a new game**. Each game has its own game id and it is represented by the class `domain.Game`. An hash map `games` is used to keep track of the ongoing games.
        - a game has a state: it starts from `WAITING_FOR_PLAYER` meaning that we are waiting for another player to join, `PLAYING` when another player joined the game and players are ready to play, `FINISHED` when the game is ended.
      - **to join an existing game**, given a game id, a user id and the symbol to be used (cross or circle)
        - When a user joins a game, a websocket is created to notify game events to the frontend. 
        - When both players joined a game and their websockets have been succesfully created and connected, a `game-started`event is notified to frontends and the game can start. 
        
      - **to make a move in a game**, specifying who wants to move (circle or cross) and where to move (x and y coordinates of the game grid, from 0 to 2)
        - Each time a new move is made, a `new-move`game event is generated and notified to users' frontends, through their websockets   
        - When the game ends, a `game-end` event is notified, displaying who won (or tie).

  
  
- **Frontend** 
  - A simple raw HTML5 page, providing a basic interface to register a new user, to create a new game, to join and play in a game.
    - To run it, open a browser at `http://localhost:8080/public/ttt.html`
  - Notes about the design and implementation: 
    - the page is `ttt.html`, located in `webroot` folder.  
    - the Javascript code makes HTTP requests. 
    - A websocket is used to get events, displyed in a text area. 
      - the websocket is created when a user succesfully joins a game.
      - events received with websocket are represented by JSON object, with an `event` field describing the type of event (`game-started`, `new-move`, `game-ended`). 
 
- Typical testing scenario, for two players:
  - Run the backend on localhost, optionally removing the exising `users.json` to reset users. 
  - Open two browser windows
    - in first one: create a game (getting `game-1` id), register a new user (getting `user-1` id), join the user (`user-1`) to the game (`game-1`)
    - in second one: register a new user (getting `user-2` id), join the user (`user-2`) to the game (`game-1`)
  - Then the two users can play, by making moves. 
   
       
