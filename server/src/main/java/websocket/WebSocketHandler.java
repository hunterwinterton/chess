package websocket;

import chess.ChessGame;
import chess.ChessMove;
import com.google.gson.Gson;
import model.GameData;
import model.AuthData;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
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
    private static final Map<Integer, CopyOnWriteArraySet<Session>> GAME_SESSIONS = new ConcurrentHashMap<>();
    private static final Map<Session, String> SESSION_TO_USER = new ConcurrentHashMap<>();
    private static final Map<Session, Integer> SESSION_TO_GAME = new ConcurrentHashMap<>();

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
        var set = GAME_SESSIONS.get(gameId);
        if (set != null) {
            set.remove(session);
            if (set.isEmpty()) {
                GAME_SESSIONS.remove(gameId);
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
        var set = GAME_SESSIONS.get(gameId);
        if (set != null) {
            for (Session s : set) {
                if (!s.equals(except)) {
                    sendMessage(s, new NotificationMessage(message));
                }
            }
        }
    }

    private void broadcastGameState(int gameId, ChessGame chess) {
        for (Session s : GAME_SESSIONS.getOrDefault(gameId, new CopyOnWriteArraySet<>())) {
            sendMessage(s, new LoadGameMessage(chess));
        }
    }

    private void broadcastNotification(int gameId, String message) {
        for (Session s : GAME_SESSIONS.getOrDefault(gameId, new CopyOnWriteArraySet<>())) {
            sendMessage(s, new NotificationMessage(message));
        }
    }

    private void notifyCheckOrMate(int gameId, ChessGame chess, GameData data) {
        if (chess.isInCheckmate(ChessGame.TeamColor.WHITE)) {
            broadcastNotification(gameId, data.whiteUsername() + " is in checkmate");
        } else if (chess.isInCheckmate(ChessGame.TeamColor.BLACK)) {
            broadcastNotification(gameId, data.blackUsername() + " is in checkmate");
        } else if (chess.isInCheck(ChessGame.TeamColor.WHITE)) {
            broadcastNotification(gameId, data.whiteUsername() + " is in check");
        } else if (chess.isInCheck(ChessGame.TeamColor.BLACK)) {
            broadcastNotification(gameId, data.blackUsername() + " is in check");
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
            case CONNECT -> handleConnect(session, cmd, gameId, user);
            case MAKE_MOVE -> handleMakeMove(session, cmd, gameId, user);
            case LEAVE -> handleLeave(session, cmd, gameId, user);
            case RESIGN -> handleResign(session, cmd, gameId, user);
            default -> sendError(session, "Error: unsupported command");
        }
    }

    private GameData validateAndLoadGame(Session session, int gameId, String user) {
        if (user == null) {
            sendError(session, "Error: invalid auth token");
            return null;
        }

        try {
            GameData data = db.getGame(gameId);
            if (data == null) {
                sendError(session, "Error: game not found");
            }
            return data;
        } catch (DataAccessException e) {
            sendError(session, "Error: could not load game");
            return null;
        }
    }

    private boolean validatePlayerRole(GameData data, String user) {
        return Objects.equals(user, data.whiteUsername())
                || Objects.equals(user, data.blackUsername());
    }

    private boolean isGameOver(GameData data, ChessGame chess) {
        boolean resigned = data.whiteUsername() == null || data.blackUsername() == null;
        return chess.isInCheckmate(ChessGame.TeamColor.WHITE)
                || chess.isInCheckmate(ChessGame.TeamColor.BLACK)
                || chess.isInStalemate(ChessGame.TeamColor.WHITE)
                || chess.isInStalemate(ChessGame.TeamColor.BLACK)
                || resigned;
    }

    private void handleConnect(Session session, UserGameCommand cmd, int gameId, String user) {
        GameData data = validateAndLoadGame(session, gameId, user);
        if (data == null) {
            return;
        }

        GAME_SESSIONS
                .computeIfAbsent(gameId, k -> new CopyOnWriteArraySet<>())
                .add(session);
        SESSION_TO_USER.put(session, user);
        SESSION_TO_GAME.put(session, gameId);

        ChessGame chess = data.game();
        sendMessage(session, new LoadGameMessage(chess));

        String role = Objects.equals(user, data.whiteUsername()) ? "WHITE"
                : Objects.equals(user, data.blackUsername()) ? "BLACK"
                : "OBSERVER";
        notifyOthers(gameId, session, user + " connected as " + role);
    }

    private void handleMakeMove(Session session, UserGameCommand cmd, int gameId, String user) {
        GameData data = validateAndLoadGame(session, gameId, user);
        if (data == null) {
            return;
        }

        ChessGame chess = data.game();
        ChessMove move = cmd.getMove();

        if (!validatePlayerRole(data, user)) {
            sendError(session, "Error: only players can move");
            return;
        }

        if (isGameOver(data, chess)) {
            sendError(session, "Error: game is over");
            return;
        }

        ChessGame.TeamColor playerColor =
                Objects.equals(user, data.whiteUsername())
                        ? ChessGame.TeamColor.WHITE
                        : ChessGame.TeamColor.BLACK;

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
        broadcastGameState(gameId, chess);
        String moveDesc = user + " moved from " +
                move.getStartPosition().toString() + " to " +
                move.getEndPosition().toString();
        notifyOthers(gameId, session, moveDesc);
        notifyCheckOrMate(gameId, chess, data);
    }

    private void handleLeave(Session session, UserGameCommand cmd, int gameId, String user) {
        GameData data = validateAndLoadGame(session, gameId, user);
        if (data == null) {
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
        SESSION_TO_USER.remove(session);
        SESSION_TO_GAME.remove(session);
        notifyOthers(gameId, session, user + " left the game");
    }

    private void handleResign(Session session, UserGameCommand cmd, int gameId, String user) {
        GameData data = validateAndLoadGame(session, gameId, user);
        if (data == null) {
            return;
        }

        if (!validatePlayerRole(data, user)) {
            sendError(session, "Error: only players can resign");
            return;
        }

        if (isGameOver(data, data.game())) {
            sendError(session, "Error: game is over");
            return;
        }

        GameData updated;
        if (Objects.equals(user, data.whiteUsername())) {
            updated = new GameData(
                    data.gameID(),
                    null,
                    data.blackUsername(),
                    data.gameName(),
                    data.game()
            );
        } else {
            updated = new GameData(
                    data.gameID(),
                    data.whiteUsername(),
                    null,
                    data.gameName(),
                    data.game()
            );
        }
        try {
            db.updateGame(updated);
        } catch (DataAccessException e) {
            sendError(session, "Error: could not update game");
            return;
        }
        broadcastNotification(gameId, user + " resigned");
    }
}