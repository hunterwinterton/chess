package dataaccess;

import model.AuthData;
import model.UserData;
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
    void createAuthGetAuthSuccess() throws DataAccessException {
        dao.createUser(new UserData(
                "hunter",
                "ignoredHash",
                "hunter@email.com"
        ));
        AuthData a = new AuthData("tokenH", "hunter");
        dao.createAuth(a);
        AuthData fetched = dao.getAuth("tokenH");
        assertNotNull(fetched);
        assertEquals("hunter", fetched.username());
    }

    @Test
    void deleteAuthSuccess() throws DataAccessException {
        dao.createUser(new UserData(
                "winter",
                "ignoredHash",
                "winter@email.com"
        ));
        AuthData a = new AuthData("tokenW", "winter");
        dao.createAuth(a);
        dao.deleteAuth("tokenW");
        assertNull(dao.getAuth("tokenW"));
    }

    @Test
    void getAuthNonexistentReturnsNull() throws DataAccessException {
        assertNull(dao.getAuth("noToken"));
    }

    @Test
    void createAuthInvalidUserThrows() {
        AuthData bad = new AuthData("badToken", "doesNotExist");
        assertThrows(DataAccessException.class, () ->
                dao.createAuth(bad)
        );
    }

    @Test
    void createAuthWithoutUserThrows() {
        AuthData bad = new AuthData("noUserToken", "ghostUser");
        assertThrows(DataAccessException.class, () -> dao.createAuth(bad));
    }

    @Test
    void createAuthDuplicateTokenThrows() throws DataAccessException {
        dao.createUser(new UserData("u1", "pass", "u1@mail.com"));
        AuthData a1 = new AuthData("dupToken", "u1");
        dao.createAuth(a1);
        assertThrows(DataAccessException.class, () -> dao.createAuth(a1));
    }

    @Test
    void deleteAuthNonexistentDoesNothing() throws DataAccessException {
        assertDoesNotThrow(() -> dao.deleteAuth("missing"));
        assertNull(dao.getAuth("missing"));
    }
}