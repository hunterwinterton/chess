package dataaccess;

import model.*;

import java.util.Collection;

public interface DataAccess {
    void clear();

    // User
    void createUser(UserData user);
    UserData getUser(String username);

    // Auth
    void createAuth(AuthData auth);
    AuthData getAuth(String authToken);
    void deleteAuth(String authToken);

    // Game
    int createGame(GameData game);
    GameData getGame(int gameID);
    Collection<GameData> listGames();
    void updateGame(GameData game);
}