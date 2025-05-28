package dataaccess;

import model.GameData;
import chess.ChessGame;
import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DAOGameTest {
    private DataAccess dao;

    @BeforeAll
    void setup() throws Exception {
        dao = new MySqlDataAccess();
    }

    @BeforeEach
    void clearBefore() throws DataAccessException {
        dao.clear();
    }

    @Test
    void createGameGetGameSuccess() throws DataAccessException {
        GameData g = new GameData(0, null, null, "MyGame", new ChessGame());
        int id = dao.createGame(g);
        GameData fetched = dao.getGame(id);
        assertNotNull(fetched);
        assertEquals("MyGame", fetched.gameName());
        assertNull(fetched.whiteUsername());
        assertNull(fetched.blackUsername());
    }

    @Test
    void listGamesReturnsAllInserted() throws DataAccessException {
        dao.createGame(new GameData(0, "hunter", null, "G1", new ChessGame()));
        dao.createGame(new GameData(0, null, "winter", "G2", new ChessGame()));
        List<GameData> list = new ArrayList<>(dao.listGames());
        assertEquals(2, list.size());
        Set<String> names = Set.of(list.get(0).gameName(), list.get(1).gameName());
        assertTrue(names.contains("G1"));
        assertTrue(names.contains("G2"));
    }

    @Test
    void getGameNonexistentReturnsNull() throws DataAccessException {
        assertNull(dao.getGame(999));
    }

    @Test
    void updateGameSuccess() throws DataAccessException {
        int id = dao.createGame(new GameData(0, null, null, "UpGame", new ChessGame()));
        GameData updated = new GameData(id, "hunter", "winter", "UpGame", new ChessGame());
        dao.updateGame(updated);
        GameData fetched = dao.getGame(id);
        assertEquals("hunter", fetched.whiteUsername());
        assertEquals("winter", fetched.blackUsername());
    }

    @Test
    void updateGameNonexistent() throws DataAccessException {
        GameData g = new GameData(42, "x", "y", "NoGame", new ChessGame());
        dao.updateGame(g);
        assertNull(dao.getGame(42));
    }

    @Test
    void listGamesEmptyWhenNone() throws DataAccessException {
        assertTrue(dao.listGames().isEmpty());
    }

    @Test
    void createGameNullThrows() {
        assertThrows(NullPointerException.class, () ->
                dao.createGame(null)
        );
    }

    @Test
    void listGamesEmptyReturnsEmpty() throws DataAccessException {
        assertTrue(dao.listGames().isEmpty());
    }

    @Test
    void updateGameNullThrows() {
        assertThrows(NullPointerException.class, () -> dao.updateGame(null));
    }
}