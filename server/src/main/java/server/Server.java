package server;

import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import dataaccess.MemoryDataAccess;
import dataaccess.MySqlDataAccess;
import handler.ClearHandler;
import handler.GameHandler;
import handler.UserHandler;
import service.ClearService;
import service.GameService;
import service.UserService;
import spark.Spark;
import websocket.WebSocketHandler;

import static spark.Spark.port;

public class Server {

    public int run(int desiredPort) {
        Spark.port(desiredPort);

        Spark.staticFiles.location("web");

        // Register your endpoints and handle exceptions here.
        final DataAccess db;
        try {
            db = new MySqlDataAccess();
        } catch (DataAccessException e) {
            throw new RuntimeException("Unable to initialize database", e);
        }

        UserService userService = new UserService(db);
        GameService gameService = new GameService(db);
        ClearService clearService = new ClearService(db);

        WebSocketHandler.configure(gameService, userService, db);
        Spark.webSocket("/ws", WebSocketHandler.class);

        UserHandler userHandler = new UserHandler(userService);
        GameHandler gameHandler = new GameHandler(gameService);
        ClearHandler clearHandler = new ClearHandler(clearService);

        // Register user endpoints
        Spark.post("/user", userHandler::register);
        Spark.post("/session", userHandler::login);
        Spark.delete("/session", userHandler::logout);

        // Register game endpoints
        Spark.post("/game", gameHandler::createGame);
        Spark.get("/game", gameHandler::listGames);
        Spark.put("/game", gameHandler::joinGame);

        // Register clear endpoint
        Spark.delete("/db", clearHandler::clear);

        Spark.init();

        Spark.awaitInitialization();
        return Spark.port();
    }

    public void stop() {
        Spark.stop();
        Spark.awaitStop();
    }
}