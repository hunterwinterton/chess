package websocket;

import chess.ChessGame;
import chess.ChessMove;
import com.google.gson.Gson;
import model.GameData;
import model.AuthData;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import service.GameService;
import service.UserService;
import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import websocket.commands.UserGameCommand;
import websocket.messages.LoadGameMessage;
import websocket.messages.NotificationMessage;
import websocket.messages.ErrorMessage;
import websocket.messages.ServerMessage;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@WebSocket
public class WebSocketHandler {
    private static final Map<Integer, CopyOnWriteArraySet<Session>> gameSessions = new ConcurrentHashMap<>();
    private static final Map<Session, String> sessionToUser = new ConcurrentHashMap<>();
    private static final Map<Session, Integer> sessionToGame = new ConcurrentHashMap<>();

    private final Gson gson = new Gson();
    private static GameService gameService;
    private static UserService userService;
    private static DataAccess db;

    public static void configure(GameService gs, UserService us, DataAccess dataAccess) {
        gameService = gs;
        userService = us;
        db = dataAccess;
    }

    private String getUsernameForToken(String authToken) {
        try {
            AuthData auth = userService.db.getAuth(authToken);
            return (auth != null) ? auth.username() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private void removeSessionFromGame(int gameId, Session session) {
        var set = gameSessions.get(gameId);
        if (set != null) {
            set.remove(session);
            if (set.isEmpty()) {
                gameSessions.remove(gameId);
            }
        }
    }

    private void sendMessage(Session session, ServerMessage msg) {
        try {
            if (session.isOpen()) {
                session.getRemote().sendString(gson.toJson(msg));
            }
        } catch (IOException ignored) {
        }
    }

    private void sendError(Session session, String msg) {
        sendMessage(session, new ErrorMessage(msg));
    }

    private void notifyOthers(int gameId, Session except, String message) {
        var set = gameSessions.get(gameId);
        if (set != null) {
            for (Session s : set) {
                if (!s.equals(except)) {
                    sendMessage(s, new NotificationMessage(message));
                }
            }
        }
    }

    @OnWebSocketConnect
    public void onConnect(Session session) {
    }

    @OnWebSocketMessage
    public void onMessage(Session session, String json) {
        UserGameCommand cmd = gson.fromJson(json, UserGameCommand.class);
        int gameId = cmd.getGameID();
        String user = getUsernameForToken(cmd.getAuthToken());
        switch (cmd.getCommandType()) {
            case CONNECT -> {
                if (user == null) {
                    sendError(session, "Error: invalid auth token");
                    return;
                }

                GameData data;
                try {
                    data = db.getGame(gameId);
                } catch (DataAccessException e) {
                    sendError(session, "Error: could not load game");
                    return;
                }
                if (data == null) {
                    sendError(session, "Error: game not found");
                    return;
                }

                gameSessions
                        .computeIfAbsent(gameId, k -> new CopyOnWriteArraySet<>())
                        .add(session);
                sessionToUser.put(session, user);
                sessionToGame.put(session, gameId);

                ChessGame chess = data.game();
                sendMessage(session, new LoadGameMessage(chess));

                String role = user.equals(data.whiteUsername()) ? "WHITE"
                        : user.equals(data.blackUsername()) ? "BLACK"
                        : "OBSERVER";
                notifyOthers(gameId, session, user + " connected as " + role);
            }
            default -> sendError(session, "Error: unsupported command");
        }
    }

    @OnWebSocketClose
    public void onClose(Session session, int statusCode, String reason) {
        Integer gameId = sessionToGame.remove(session);
        sessionToUser.remove(session);
        if (gameId != null) {
            removeSessionFromGame(gameId, session);
        }
    }
}