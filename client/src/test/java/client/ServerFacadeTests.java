package client;

import model.AuthData;
import org.junit.jupiter.api.*;
import server.Server;

import static org.junit.jupiter.api.Assertions.*;


public class ServerFacadeTests {

    private static Server server;
    private static ServerFacade facade;

    @BeforeAll
    public static void init() {
        server = new Server();
        var port = server.run(0);
        System.out.println("Started test HTTP server on " + port);
        String baseUrl = "http://localhost:" + port;
        facade = new ServerFacade(baseUrl);
    }

    @AfterAll
    static void stopServer() {
        server.stop();
    }

    @BeforeEach
    public void clearDatabase() throws Exception {
        facade.clear();
        facade.setAuthToken(null);
    }

    @Test
    void registerPositive() throws Exception {
        AuthData auth = facade.register("hunter", "password", "hunter@example.com");
        assertNotNull(auth.authToken());
        assertEquals("hunter", auth.username());
    }

    @Test
    void registerNegative_duplicateUsername() throws Exception {
        facade.register("hunter1", "password", "hunter1@example.com");
        assertThrows(Exception.class, () ->
                facade.register("hunter1", "password", "hunter1duplicate@example.com")
        );
    }

    @Test
    void loginPositive() throws Exception {
        facade.register("hunter2", "password", "hunter2@example.com");
        AuthData auth = facade.login("hunter2", "password");
        assertNotNull(auth.authToken());
        assertEquals("hunter2", auth.username());
    }

    @Test
    void loginNegative_invalidCredentials() {
        assertThrows(Exception.class, () ->
                facade.login("nonexistent", "wrongpass")
        );
    }

    @Test
    void logoutPositive() throws Exception {
        AuthData auth = facade.register("hunter3", "password", "hunter3@example.com");
        facade.setAuthToken(auth.authToken());
        facade.logout();
    }

    @Test
    void logoutNegative_invalidToken() {
        facade.setAuthToken("invalid_token");
        assertThrows(Exception.class, () ->
                facade.logout()
        );
    }

}
