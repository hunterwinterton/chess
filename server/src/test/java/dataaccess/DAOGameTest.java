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
    void createGame_and_getGame_success() throws DataAccessException {
        GameData g = new GameData(0, null, null, "MyGame", new ChessGame());
        int id = dao.createGame(g);
        GameData fetched = dao.getGame(id);
        assertNotNull(fetched);
        assertEquals("MyGame", fetched.gameName());
        assertNull(fetched.whiteUsername());
        assertNull(fetched.blackUsername());
    }

    @Test
    void listGames_returnsAllInserted() throws DataAccessException {
        dao.createGame(new GameData(0, "hunter", null, "G1", new ChessGame()));
        dao.createGame(new GameData(0, null, "winter", "G2", new ChessGame()));
        List<GameData> list = new ArrayList<>(dao.listGames());
        assertEquals(2, list.size());
        Set<String> names = Set.of(list.get(0).gameName(), list.get(1).gameName());
        assertTrue(names.contains("G1"));
        assertTrue(names.contains("G2"));
    }

    @Test
    void getGame_nonexistent_returnsNull() throws DataAccessException {
        assertNull(dao.getGame(999));
    }

    @Test
    void updateGame_success() throws DataAccessException {
        int id = dao.createGame(new GameData(0, null, null, "UpGame", new ChessGame()));
        GameData updated = new GameData(id, "hunter", "winter", "UpGame", new ChessGame());
        dao.updateGame(updated);
        GameData fetched = dao.getGame(id);
        assertEquals("hunter", fetched.whiteUsername());
        assertEquals("winter", fetched.blackUsername());
    }

    @Test
    void updateGame_nonexistent() {
        GameData g = new GameData(42, "x", "y", "NoGame", new ChessGame());
        assertThrows(DataAccessException.class, () ->
                dao.updateGame(g)
        );
    }
}