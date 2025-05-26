package dataaccess;

import com.google.gson.Gson;
import model.AuthData;
import model.UserData;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.SQLException;

public class MySqlDataAccess implements DataAccess {
    private final Gson gson = new Gson();
    private final String[] createStatements = {
            """
        CREATE TABLE IF NOT EXISTS user (
          username VARCHAR(100) PRIMARY KEY,
          password VARCHAR(100) NOT NULL,
          email VARCHAR(100) NOT NULL
        )
        """,
            """
        CREATE TABLE IF NOT EXISTS auth (
          token VARCHAR(100) PRIMARY KEY,
          username VARCHAR(100),
          FOREIGN KEY(username) REFERENCES user(username) ON DELETE CASCADE
        )
        """,
            """
        CREATE TABLE IF NOT EXISTS game (
          id INT AUTO_INCREMENT PRIMARY KEY,
          whiteUsername VARCHAR(100),
          blackUsername VARCHAR(100),
          gameName VARCHAR(100),
          gameState TEXT
        )
        """
    };

    public MySqlDataAccess() throws DataAccessException {
        DatabaseManager.createDatabase();
    }

    public void clear() {
        executeUpdate("TRUNCATE TABLE auth");
        executeUpdate("TRUNCATE TABLE game");
        executeUpdate("TRUNCATE TABLE user");
    }

    private void executeUpdate(String statement, Object... params) {
    }

    public void createUser(UserData user) throws DataAccessException {
        String hashed = BCrypt.hashpw(user.password(), BCrypt.gensalt());
        executeUpdate(
                "INSERT INTO user(username,password,email) VALUES(?,?,?)",
                user.username(), hashed, user.email()
        );
    }

    public UserData getUser(String username) throws DataAccessException {
        String sql = "SELECT username,password,email FROM user WHERE username=?";
        try (var conn = DatabaseManager.getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (var rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new UserData(
                            rs.getString("username"),
                            rs.getString("password"),
                            rs.getString("email")
                    );
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException(e.getMessage());
        }
        return null;
    }

    public void createAuth(AuthData auth) throws DataAccessException {
        executeUpdate(
                "INSERT INTO auth(token,username) VALUES(?,?)",
                auth.authToken(), auth.username()
        );
    }

    public AuthData getAuth(String token) throws DataAccessException {
        String sql = "SELECT token,username FROM auth WHERE token=?";
        try (var conn = DatabaseManager.getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setString(1, token);
            try (var rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new AuthData(
                            rs.getString("token"),
                            rs.getString("username")
                    );
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException(e.getMessage());
        }
        return null;
    }

    public void deleteAuth(String token) throws DataAccessException {
        executeUpdate("DELETE FROM auth WHERE token=?", token);
    }
}