package dataaccess;

import com.google.gson.Gson;

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
}