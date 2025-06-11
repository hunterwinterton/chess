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
import java.util.Objects;

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

            case MAKE_MOVE -> {
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

                ChessGame chess = data.game();
                ChessMove move = cmd.getMove();

                boolean isWhite = Objects.equals(user, data.whiteUsername());
                boolean isBlack = Objects.equals(user, data.blackUsername());

                if (!isWhite && !isBlack) {
                    sendError(session, "Error: only players can move");
                    return;
                }

                boolean resigned = data.whiteUsername() == null || data.blackUsername() == null;
                boolean over = chess.isInCheckmate(ChessGame.TeamColor.WHITE)
                        || chess.isInCheckmate(ChessGame.TeamColor.BLACK)
                        || chess.isInStalemate(ChessGame.TeamColor.WHITE)
                        || chess.isInStalemate(ChessGame.TeamColor.BLACK)
                        || resigned;
                if (over) {
                    sendError(session, "Error: game is over");
                    return;
                }

                ChessGame.TeamColor playerColor = isWhite ? ChessGame.TeamColor.WHITE : ChessGame.TeamColor.BLACK;
                if (chess.getTeamTurn() != playerColor) {
                    sendError(session, "Error: not your turn");
                    return;
                }

                if (move == null) {
                    sendError(session, "Error: missing move");
                    return;
                }
                try {
                    chess.makeMove(move);
                } catch (Exception e) {
                    sendError(session, "Error: invalid move");
                    return;
                }
                GameData updated = new GameData(
                        data.gameID(),
                        data.whiteUsername(),
                        data.blackUsername(),
                        data.gameName(),
                        chess
                );
                try {
                    db.updateGame(updated);
                } catch (DataAccessException e) {
                    sendError(session, "Error: could not update game");
                    return;
                }
                for (Session s : gameSessions.getOrDefault(gameId, new CopyOnWriteArraySet<>())) {
                    sendMessage(s, new LoadGameMessage(chess));
                }
                String moveDesc = user + " moved from " +
                        move.getStartPosition().toString() + " to " +
                        move.getEndPosition().toString();
                notifyOthers(gameId, session, moveDesc);

                if (chess.isInCheckmate(ChessGame.TeamColor.WHITE)) {
                    for (Session s : gameSessions.getOrDefault(gameId, new CopyOnWriteArraySet<>())) {
                        sendMessage(s, new NotificationMessage(data.whiteUsername() + " is in checkmate"));
                    }
                } else if (chess.isInCheckmate(ChessGame.TeamColor.BLACK)) {
                    for (Session s : gameSessions.getOrDefault(gameId, new CopyOnWriteArraySet<>())) {
                        sendMessage(s, new NotificationMessage(data.blackUsername() + " is in checkmate"));
                    }
                } else if (chess.isInCheck(ChessGame.TeamColor.WHITE)) {
                    for (Session s : gameSessions.getOrDefault(gameId, new CopyOnWriteArraySet<>())) {
                        sendMessage(s, new NotificationMessage(data.whiteUsername() + " is in check"));
                    }
                } else if (chess.isInCheck(ChessGame.TeamColor.BLACK)) {
                    for (Session s : gameSessions.getOrDefault(gameId, new CopyOnWriteArraySet<>())) {
                        sendMessage(s, new NotificationMessage(data.blackUsername() + " is in check"));
                    }
                }
            }

            case LEAVE -> {
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

                boolean isWhite = Objects.equals(user, data.whiteUsername());
                boolean isBlack = Objects.equals(user, data.blackUsername());
                GameData updated = data;
                if (isWhite) {
                    updated = new GameData(
                            data.gameID(),
                            null,
                            data.blackUsername(),
                            data.gameName(),
                            data.game()
                    );
                } else if (isBlack) {
                    updated = new GameData(
                            data.gameID(),
                            data.whiteUsername(),
                            null,
                            data.gameName(),
                            data.game()
                    );
                }
                if (isWhite || isBlack) {
                    try {
                        db.updateGame(updated);
                    } catch (DataAccessException e) {
                        sendError(session, "Error: could not update game");
                        return;
                    }
                }

                removeSessionFromGame(gameId, session);
                sessionToUser.remove(session);
                sessionToGame.remove(session);
                notifyOthers(gameId, session, user + " left the game");
            }

            case RESIGN -> {
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

                boolean isWhite = Objects.equals(user, data.whiteUsername());
                boolean isBlack = Objects.equals(user, data.blackUsername());

                if (!isWhite && !isBlack) {
                    sendError(session, "Error: only players can resign");
                    return;
                }

                ChessGame chess = data.game();

                boolean resigned = data.whiteUsername() == null || data.blackUsername() == null;
                boolean over = chess.isInCheckmate(ChessGame.TeamColor.WHITE)
                        || chess.isInCheckmate(ChessGame.TeamColor.BLACK)
                        || chess.isInStalemate(ChessGame.TeamColor.WHITE)
                        || chess.isInStalemate(ChessGame.TeamColor.BLACK)
                        || resigned;
                if (over) {
                    sendError(session, "Error: game is over");
                    return;
                }

                GameData updated;
                if (isWhite) {
                    updated = new GameData(
                            data.gameID(),
                            null,
                            data.blackUsername(),
                            data.gameName(),
                            chess
                    );
                } else {
                    updated = new GameData(
                            data.gameID(),
                            data.whiteUsername(),
                            null,
                            data.gameName(),
                            chess
                    );
                }
                try {
                    db.updateGame(updated);
                } catch (DataAccessException e) {
                    sendError(session, "Error: could not update game");
                    return;
                }
                for (Session s : gameSessions.getOrDefault(gameId, new CopyOnWriteArraySet<>())) {
                    sendMessage(s, new NotificationMessage(user + " resigned"));
                }
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