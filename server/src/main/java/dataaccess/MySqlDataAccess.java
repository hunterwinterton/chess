package dataaccess;

import chess.ChessGame;
import com.google.gson.Gson;
import model.AuthData;
import model.GameData;
import model.UserData;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;

import static java.sql.Statement.RETURN_GENERATED_KEYS;
import static java.sql.Types.NULL;

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
        try (var conn = DatabaseManager.getConnection()) {
            configureDatabase();
        } catch (SQLException e) {
            throw new DataAccessException("Error: " + e.getMessage());
        }
    }


    public void clear() throws DataAccessException {
        executeUpdate("TRUNCATE TABLE auth");
        executeUpdate("TRUNCATE TABLE game");
        executeUpdate("TRUNCATE TABLE user");
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
            throw new DataAccessException("Error: " + e.getMessage());
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
            throw new DataAccessException("Error: " + e.getMessage());
        }
        return null;
    }

    public void deleteAuth(String token) throws DataAccessException {
        executeUpdate("DELETE FROM auth WHERE token=?", token);
    }

    public int createGame(GameData game) throws DataAccessException {
        String json = gson.toJson(game.game());
        return executeInsert(
                "INSERT INTO game(whiteUsername,blackUsername,gameName,gameState) VALUES(?,?,?,?)",
                game.whiteUsername(), game.blackUsername(), game.gameName(), json
        );
    }

    public GameData getGame(int id) throws DataAccessException {
        String sql = "SELECT * FROM game WHERE id=?";
        try (var conn = DatabaseManager.getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (var rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new GameData(
                            id,
                            rs.getString("whiteUsername"),
                            rs.getString("blackUsername"),
                            rs.getString("gameName"),
                            gson.fromJson(rs.getString("gameState"), ChessGame.class)
                    );
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error: " + e.getMessage());
        }
        return null;
    }

    public Collection<GameData> listGames() throws DataAccessException {
        Collection<GameData> games = new ArrayList<>();
        String sql = "SELECT * FROM game";
        try (var conn = DatabaseManager.getConnection();
             var ps = conn.prepareStatement(sql);
             var rs = ps.executeQuery()) {
            while (rs.next()) {
                games.add(new GameData(
                        rs.getInt("id"),
                        rs.getString("whiteUsername"),
                        rs.getString("blackUsername"),
                        rs.getString("gameName"),
                        gson.fromJson(rs.getString("gameState"), ChessGame.class)
                ));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error: " + e.getMessage());
        }
        return games;
    }

    public void updateGame(GameData game) throws DataAccessException {
        String json = gson.toJson(game.game());
        executeUpdate(
                "UPDATE game SET whiteUsername=?,blackUsername=?,gameName=?,gameState=? WHERE id=?",
                game.whiteUsername(), game.blackUsername(), game.gameName(), json, game.gameID()
        );
    }

    // HELPERS

    private void configureDatabase() throws DataAccessException {
        try (var conn = DatabaseManager.getConnection()) {
            for (String stmt : createStatements) {
                try (var ps = conn.prepareStatement(stmt)) {
                    ps.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error: " + e.getMessage());
        }
    }

    private int executeInsert(String sql, Object... params) throws DataAccessException {
        try (var conn = DatabaseManager.getConnection();
             var ps = conn.prepareStatement(sql, RETURN_GENERATED_KEYS)) {
            for (int i = 0; i < params.length; i++) {
                if (params[i] == null) {
                    ps.setNull(i+1, NULL);
                } else {
                    ps.setObject(i+1, params[i]);
                }
            }
            ps.executeUpdate();
            try (var rs = ps.getGeneratedKeys()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error: " + e.getMessage());
        }
    }

    private void executeUpdate(String sql, Object... params) throws DataAccessException {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                if (params[i] == null) {
                    ps.setNull(i+1, NULL);
                } else {
                    ps.setObject(i+1, params[i]);
                }
            }
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Error: " + e.getMessage());
        }
    }
}