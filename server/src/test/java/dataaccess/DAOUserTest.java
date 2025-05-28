package dataaccess;

import model.UserData;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DAOUserTest {
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
    void createUser_success() throws DataAccessException {
        UserData u = new UserData("hunter", "pass", "hunter@email.com");
        dao.createUser(u);
        UserData fetched = dao.getUser("hunter");
        assertNotNull(fetched);
        assertEquals("hunter", fetched.username());
        assertEquals("hunter@email.com", fetched.email());
    }

    @Test
    void createUser_duplicateUsername() throws DataAccessException {
        dao.createUser(new UserData("winter", "pass", "winter@email.com"));
        assertThrows(DataAccessException.class, () ->
                dao.createUser(new UserData("winter", "pass", "duplicate@email.com"))
        );
    }

    @Test
    void getUser_nonexistent_returnsNull() throws DataAccessException {
        assertNull(dao.getUser("noSuchUser"));
    }
}