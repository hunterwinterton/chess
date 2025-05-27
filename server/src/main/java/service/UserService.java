package service;

import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import model.AuthData;
import model.UserData;
import org.mindrot.jbcrypt.BCrypt;

import java.util.UUID;

public class UserService {
    private final DataAccess db;

    public UserService(DataAccess db) {
        this.db = db;
    }

    public AuthData register(UserData user) throws DataAccessException {
        if (db.getUser(user.username()) != null) {
            throw new DataAccessException("Error: already taken");
        }
        db.createUser(user);
        return createAuth(user.username());
    }

    public AuthData login(String username, String password) throws DataAccessException {
        UserData user = db.getUser(username);
        if (user == null || !BCrypt.checkpw(password, user.password())) {
            throw new DataAccessException("Error: unauthorized");
        }
        return createAuth(username);
    }

    public void logout(String authToken) throws DataAccessException {
        if (db.getAuth(authToken) == null) {
            throw new DataAccessException("Error: unauthorized");
        }
        db.deleteAuth(authToken);
    }

    private AuthData createAuth(String username) throws DataAccessException {
        String authToken = UUID.randomUUID().toString();
        AuthData auth = new AuthData(authToken, username);
        db.createAuth(auth);
        return auth;
    }
}
