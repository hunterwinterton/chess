package websocket;

import chess.ChessGame;
import com.google.gson.Gson;
import model.GameData;
import model.AuthData;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;
import service.GameService;
import service.UserService;
import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import websocket.commands.UserGameCommand;
import websocket.messages.*;

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
        CopyOnWriteArraySet<Session> set = gameSessions.get(gameId);
        if (set != null) {
            set.remove(session);
            if (set.isEmpty()) {
                gameSessions.remove(gameId);
            }
        }
    }

    private void sendMessage(Session session, ServerMessage msg) {
        try {
            if (session.isOpen()) session.getRemote().sendString(gson.toJson(msg));
        } catch (IOException ignored) {}
    }

    private void sendError(Session session, String msg) {
        sendMessage(session, new ErrorMessage(msg));
    }

    private void notifyAll(int gameId, ServerMessage msg) {
        CopyOnWriteArraySet<Session> set = gameSessions.get(gameId);
        if (set != null) for (Session s : set) sendMessage(s, msg);
    }

    private void notifyAll(int gameId, String message) {
        notifyAll(gameId, new NotificationMessage(message));
    }

    private void notifyOthers(int gameId, Session except, String message) {
        CopyOnWriteArraySet<Session> set = gameSessions.get(gameId);
        if (set != null) for (Session s : set) if (!s.equals(except)) sendMessage(s, new NotificationMessage(message));
    }

    private boolean isPlayer(String username, GameData game) {
        return username.equals(game.whiteUsername()) || username.equals(game.blackUsername());
    }

    private String getRole(String username, GameData game) {
        if (username.equals(game.whiteUsername())) return "WHITE";
        if (username.equals(game.blackUsername())) return "BLACK";
        return "OBSERVER";
    }

    private String getUsernameForColor(GameData game, ChessGame.TeamColor color) {
        return color == ChessGame.TeamColor.WHITE ?
                (game.whiteUsername() != null ? game.whiteUsername() : "White") :
                (game.blackUsername() != null ? game.blackUsername() : "Black");
    }

    private boolean isGameOver(ChessGame chess) {
        return chess.isInCheckmate(ChessGame.TeamColor.WHITE)
                || chess.isInCheckmate(ChessGame.TeamColor.BLACK)
                || chess.isInStalemate(ChessGame.TeamColor.WHITE)
                || chess.isInStalemate(ChessGame.TeamColor.BLACK);
    }
}