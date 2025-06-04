package client;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import model.AuthData;
import model.UserData;
import model.GameData;

public class ServerFacade {
    private final String serverUrl;
    private String authToken;
    private final Gson gson = new Gson();

    public ServerFacade(String port) {
        this.serverUrl = "http://localhost:" + port;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    public AuthData register(String username, String password, String email) throws Exception {
        UserData user = new UserData(username, password, email);
        return makeRequest("POST", "/user", user, AuthData.class);
    }

    public AuthData login(String username, String password) throws Exception {
        JsonObject jo = new JsonObject();
        jo.addProperty("username", username);
        jo.addProperty("password", password);
        return makeRequest("POST", "/session", jo, AuthData.class);
    }

    public void logout() throws Exception {
        makeRequest("DELETE", "/session", null, null);
    }

    public int createGame(String gameName) throws Exception {
        CreateGameRequest req = new CreateGameRequest(gameName);
        CreateGameResponse resp = makeRequest("POST", "/game", req, CreateGameResponse.class);
        assert resp != null;
        return resp.getGameID();
    }

    public ListGamesResponse listGames() throws Exception {
        return makeRequest("GET", "/game", null, ListGamesResponse.class);
    }

    public void joinGame(int gameID, String playerColor) throws Exception {
        JoinGameRequest req = new JoinGameRequest(playerColor, gameID);
        makeRequest("PUT", "/game", req, null);
    }

    public void clear() throws Exception {
        makeRequest("DELETE", "/db", null, null);
    }

    private <T> T makeRequest(String method, String endpoint, Object payloadObj, Class<T> responseClass) throws Exception {
        HttpResponse<String> response;
        try (HttpClient client = HttpClient.newHttpClient()) {
            String requestBody = payloadObj == null ? "" : gson.toJson(payloadObj);

            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(serverUrl + endpoint))
                    .header("Content-Type", "application/json");

            if (authToken != null) {
                builder.header("Authorization", authToken);
            }

            switch (method) {
                case "POST" -> builder.POST(HttpRequest.BodyPublishers.ofString(requestBody));
                case "GET" -> builder.GET();
                case "PUT" -> builder.PUT(HttpRequest.BodyPublishers.ofString(requestBody));
                case "DELETE" -> builder.DELETE();
                default -> throw new Exception("Invalid HTTP method: " + method);
            }

            HttpRequest httpRequest = builder.build();
            response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        }

        if (response.statusCode() >= 400) {
            throw new Exception("Error: " + response.statusCode() + " - " + response.body());
        }

        if (responseClass != null) {
            return gson.fromJson(response.body(), responseClass);
        } else {
            return null;
        }
    }

    public record CreateGameRequest(String gameName) {
    }

    public static class CreateGameResponse {
        private int gameID;

        public int getGameID() {
            return gameID;
        }

        public void setGameID(int gameID) {
            this.gameID = gameID;
        }
    }

    public record JoinGameRequest(String playerColor, int gameID) {
    }

    public static class ListGamesResponse {
        private java.util.List<GameData> games;

        public java.util.List<GameData> getGames() {
            return games;
        }

        public void setGames(java.util.List<GameData> games) {
            this.games = games;
        }
    }
}