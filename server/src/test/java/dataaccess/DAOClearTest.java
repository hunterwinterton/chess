package dataaccess;

import model.UserData;
import model.AuthData;
import model.GameData;
import chess.ChessGame;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DAOClearTest {
    private DataAccess dao;

    @BeforeAll
    void setup() throws Exception {
        dao = new MySqlDataAccess();
        dao.clear();
    }

    @Test
    void clearRemovesAllData() throws DataAccessException {

        dao.createUser(new UserData("hunter", "pass", "hunter@email.com"));
        dao.createUser(new UserData("winter", "pass", "winter@email.com"));

        dao.createAuth(new AuthData("tokenH", "hunter"));
        dao.createAuth(new AuthData("tokenW", "winter"));

        dao.createGame(new GameData(0, "hunter", "winter", "TestGame", new ChessGame()));

        dao.clear();

        assertNull(dao.getUser("hunter"));
        assertNull(dao.getUser("winter"));
        assertNull(dao.getAuth("tokenH"));
        assertNull(dao.getAuth("tokenW"));
        assertTrue(dao.listGames().isEmpty());
    }
}