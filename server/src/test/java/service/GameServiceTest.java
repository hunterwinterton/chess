package service;

import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import dataaccess.MySqlDataAccess;
import model.AuthData;
import model.GameData;
import model.UserData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

class GameServiceTest {

    private GameService gameService;
    private DataAccess db;
    private String authToken;

    @BeforeEach
    void setUp() throws DataAccessException {
        db = new MySqlDataAccess();
        db.clear();
        gameService = new GameService(db);

        UserData user = new UserData("hunter", "byu", "email");
        db.createUser(user);
        AuthData auth = new AuthData("valid-token", "hunter");
        db.createAuth(auth);
        authToken = auth.authToken();
    }

    @Test
    void createGameSuccess() throws DataAccessException {
        int gameID = gameService.createGame(authToken, "chess1");
        assertTrue(gameID > 0);
    }

    @Test
    void createGameUnauthorized() {
        assertThrows(DataAccessException.class, () -> gameService.createGame("invalid-token", "chess1"));
    }

    @Test
    void listGamesSuccess() throws DataAccessException {
        gameService.createGame(authToken, "chess1");
        Collection<GameData> games = gameService.listGames(authToken);
        assertEquals(1, games.size());
        assertEquals("chess1", games.iterator().next().gameName());
    }

    @Test
    void listGamesUnauthorized() {
        assertThrows(DataAccessException.class, () -> gameService.listGames("invalid-token"));
    }

    @Test
    void joinGameSuccess() throws DataAccessException {
        int gameID = gameService.createGame(authToken, "chess1");
        assertDoesNotThrow(() -> gameService.joinGame(authToken, gameID, "WHITE"));
    }

    @Test
    void joinGameInvalidAuth() throws DataAccessException {
        int gameID = gameService.createGame(authToken, "chess1");
        assertThrows(DataAccessException.class, () -> gameService.joinGame("invalid-token", gameID, "BLACK"));
    }

    @Test
    void joinGameTakenColor() throws DataAccessException {
        int gameID = gameService.createGame(authToken, "chess1");
        gameService.joinGame(authToken, gameID, "BLACK");

        UserData user2 = new UserData("user2", "pass", "email");
        db.createUser(user2);
        AuthData auth2 = new AuthData("token2", "user2");
        db.createAuth(auth2);

        assertThrows(DataAccessException.class, () -> gameService.joinGame("token2", gameID, "BLACK"));
    }

    @Test
    void joinGameInvalidColor() throws DataAccessException {
        int gameID = gameService.createGame(authToken, "chess1");
        assertThrows(DataAccessException.class, () -> gameService.joinGame(authToken, gameID, "PURPLE"));
    }
}