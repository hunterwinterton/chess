package service;

import chess.ChessGame;
import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import model.AuthData;
import model.GameData;

import java.util.Collection;

public class GameService {
    private final DataAccess db;

    public GameService(DataAccess db) {
        this.db = db;
    }

    public int createGame(String authToken, String gameName) throws DataAccessException {
        validateAuth(authToken);
        if (gameName == null || gameName.isEmpty()) {
            throw new DataAccessException("Error: bad request");
        }

        GameData newGame = new GameData(0, null, null, gameName, new ChessGame());
        return db.createGame(newGame);
    }

    public Collection<GameData> listGames(String authToken) throws DataAccessException {
        validateAuth(authToken);
        return db.listGames();
    }

    public void joinGame(String authToken, int gameID, String playerColor) throws DataAccessException {
        AuthData auth = validateAuth(authToken);
        GameData game = db.getGame(gameID);

        if (game == null) {
            throw new DataAccessException("Error: Game not found");
        }

        String username = auth.username();
        String white = game.whiteUsername();
        String black = game.blackUsername();

        if ("WHITE".equalsIgnoreCase(playerColor)) {
            if (white != null) {
                throw new DataAccessException("Error: Color already taken");
            }
            db.updateGame(new GameData(gameID, username, black, game.gameName(), game.game()));
        } else if ("BLACK".equalsIgnoreCase(playerColor)) {
            if (black != null) {
                throw new DataAccessException("Error: Color already taken");
            }
            db.updateGame(new GameData(gameID, white, username, game.gameName(), game.game()));
        } else {
            throw new DataAccessException("Error: bad request");
        }
    }

    private AuthData validateAuth(String token) throws DataAccessException {
        AuthData auth = db.getAuth(token);
        if (auth == null) {
            throw new DataAccessException("Error: unauthorized");
        }
        return auth;
    }
}