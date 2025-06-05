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
        facade = new ServerFacade(port);
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
    void registerNegativeDuplicateUsername() throws Exception {
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
    void loginNegativeInvalidCredentials() {
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
    void logoutNegativeInvalidToken() {
        facade.setAuthToken("invalid_token");
        assertThrows(Exception.class, () ->
                facade.logout()
        );
    }

    @Test
    void createGamePositive() throws Exception {
        AuthData auth = facade.register("hunter4", "password", "hunter4@example.com");
        facade.setAuthToken(auth.authToken());
        int gameId = facade.createGame("game1");
        assertTrue(gameId > 0);
    }

    @Test
    void createGameNegativeNotLoggedIn() {
        assertThrows(Exception.class, () ->
                facade.createGame("game1")
        );
    }

    @Test
    public void listGamesPositive() throws Exception {
        AuthData a5 = facade.register("hunter5", "hunterpass5", "h5@example.com");
        facade.setAuthToken(a5.authToken());
        facade.createGame("onlyGame");
        assertNotNull(facade.listGames());
    }

    @Test
    void listGamesNegativeNotLoggedIn() {
        assertThrows(Exception.class, () ->
                facade.listGames()
        );
    }

    @Test
    void joinGamePositive() throws Exception {
        AuthData authA = facade.register("hunter6", "password", "hunter6@example.com");
        facade.setAuthToken(authA.authToken());
        int gameId = facade.createGame("game3");

        AuthData authB = facade.register("hunter7", "password", "hunter7@example.com");
        facade.setAuthToken(authB.authToken());
        facade.joinGame(gameId, "WHITE");
    }

    @Test
    void joinGameNegativeInvalidGameId() throws Exception {
        AuthData auth = facade.register("hunter8", "password", "hunter8@example.com");
        facade.setAuthToken(auth.authToken());
        assertThrows(Exception.class, () ->
                facade.joinGame(9999, "WHITE")
        );
    }

}