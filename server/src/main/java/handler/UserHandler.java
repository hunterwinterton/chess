package handler;

import com.google.gson.Gson;
import service.UserService;
import model.UserData;
import spark.Request;
import spark.Response;

import java.util.Map;

public class UserHandler {
    private final UserService service;
    private final Gson gson = new Gson();

    public UserHandler(UserService service) {
        this.service = service;
    }

    public Object register(Request req, Response res) {
        try {
            UserData user = gson.fromJson(req.body(), UserData.class);
            if (user == null || user.username() == null || user.password() == null || user.email() == null) {
                throw new IllegalArgumentException("Error: bad request");
            }
            var auth = service.register(user);
            res.status(200);
            return gson.toJson(auth);
        } catch (Exception e) {
            return handleException(e, res);
        }
    }

    public Object login(Request req, Response res) {
        try {
            Map<?, ?> body = gson.fromJson(req.body(), Map.class);

            if (body == null || body.get("username") == null || body.get("password") == null) {
                throw new IllegalArgumentException("Error: bad request");
            }

            String username = body.get("username").toString();
            String password = body.get("password").toString();

            var auth = service.login(username, password);
            res.status(200);
            return gson.toJson(auth);
        } catch (Exception e) {
            return handleException(e, res);
        }
    }

    public Object logout(Request req, Response res) {
        try {
            String token = req.headers("Authorization");
            if (token == null || token.isEmpty()) {
                throw new IllegalArgumentException("Error: unauthorized");
            }

            service.logout(token);
            res.status(200);
            return "{}";
        } catch (Exception e) {
            return handleException(e, res);
        }
    }

    private Object handleException(Exception e, Response res) {
        if (e.getMessage().contains("already taken")) {
            res.status(403);
        } else if (e.getMessage().contains("unauthorized")) {
            res.status(401);
        } else if (e.getMessage().contains("bad request")) {
            res.status(400);
        } else {
            res.status(500);
        }
        return gson.toJson(Map.of("message", "Error: " + e.getMessage()));
    }
}