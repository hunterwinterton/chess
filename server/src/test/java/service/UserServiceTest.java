package service;

import dataaccess.*;
import model.AuthData;
import model.UserData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserServiceTest {
    private UserService service;
    private MemoryDataAccess db;

    @BeforeEach
    void setup() {
        db = new MemoryDataAccess();
        service = new UserService(db);
    }

    @Test
    void registerSuccess() throws DataAccessException {
        var auth = service.register(new UserData("hunter", "byu", "email"));
        assertNotNull(auth.authToken());
        assertEquals("hunter", auth.username());
    }

    @Test
    void registerTaken() throws DataAccessException {
        service.register(new UserData("hunter", "byu", "email"));
        assertThrows(DataAccessException.class,
                () -> service.register(new UserData("hunter", "byu", "email")));
    }

    @Test
    void loginSuccess() throws DataAccessException {
        service.register(new UserData("hunter", "byu", "email"));
        var auth = service.login("hunter", "byu");
        assertNotNull(auth.authToken());
    }

    @Test
    void loginWrongPassword() throws DataAccessException {
        service.register(new UserData("hunter", "byu", "email"));
        assertThrows(DataAccessException.class,
                () -> service.login("hunter", "wrong"));
    }

    @Test
    void logoutSuccess() throws DataAccessException {
        var auth = service.register(new UserData("hunter", "byu", "email"));
        assertDoesNotThrow(() -> service.logout(auth.authToken()));
    }

    @Test
    void logoutInvalidToken() {
        assertThrows(DataAccessException.class,
                () -> service.logout("invalid-token"));
    }
}