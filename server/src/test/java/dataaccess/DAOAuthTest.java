package dataaccess;

import model.AuthData;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DAOAuthTest {
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
    void createAuth_and_getAuth_success() throws DataAccessException {
        AuthData a = new AuthData("tokenH", "hunter");
        dao.createAuth(a);
        AuthData fetched = dao.getAuth("tokenH");
        assertNotNull(fetched);
        assertEquals("hunter", fetched.username());
    }

    @Test
    void deleteAuth_success() throws DataAccessException {
        AuthData a = new AuthData("tokenW", "winter");
        dao.createAuth(a);
        dao.deleteAuth("tokenW");
        assertNull(dao.getAuth("tokenW"));
    }

    @Test
    void getAuth_nonexistent_returnsNull() throws DataAccessException {
        assertNull(dao.getAuth("noToken"));
    }
}