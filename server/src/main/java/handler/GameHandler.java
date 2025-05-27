package handler;

import com.google.gson.Gson;
import dataaccess.DataAccessException;
import service.GameService;
import spark.Request;
import spark.Response;

import java.util.Map;

public class GameHandler {
    private final GameService service;
    private final Gson gson = new Gson();

    public GameHandler(GameService service) {
        this.service = service;
    }

    public Object createGame(Request req, Response res) {
        try {
            String authToken = req.headers("Authorization");
            Map<?, ?> body = gson.fromJson(req.body(), Map.class);
            String gameName = body.get("gameName").toString();

            var gameID = service.createGame(authToken, gameName);
            res.status(200);
            return gson.toJson(Map.of("gameID", gameID));
        } catch (Exception e) {
            return handleException(e, res);
        }
    }

    public Object listGames(Request req, Response res) {
        try {
            String authToken = req.headers("Authorization");
            var games = service.listGames(authToken);
            res.status(200);
            return gson.toJson(Map.of("games", games));
        } catch (Exception e) {
            return handleException(e, res);
        }
    }

    public Object joinGame(Request req, Response res) {
        try {
            String authToken = req.headers("Authorization");
            Map<?, ?> body = gson.fromJson(req.body(), Map.class);
            String playerColor = body.get("playerColor").toString();
            int gameID = ((Double) body.get("gameID")).intValue();

            service.joinGame(authToken, gameID, playerColor);
            res.status(200);
            return "{}";
        } catch (Exception e) {
            return handleException(e, res);
        }
    }

    private Object handleException(Exception e, Response res) {
        String msg = e.getMessage().toLowerCase();
        if (msg.contains("already taken")) {
            res.status(403);
        } else if (msg.contains("unauthorized")) {
            res.status(401);
        } else if (msg.contains("not found")) {
            res.status(404);
        } else {
            res.status(400);
        }
        return gson.toJson(Map.of("message", "Error: " + e.getMessage()));
    }
}