package id.seria.collection.managers;

import id.seria.collection.SeriaCollectionPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {

    private final SeriaCollectionPlugin plugin;
    private Connection connection;

    public DatabaseManager(SeriaCollectionPlugin plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        try {
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }

            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dataFolder + "/data.db");

            createTable();
        } catch (ClassNotFoundException | SQLException e) {
            plugin.getLogger().severe("Could not initialize SQLite database!");
            e.printStackTrace();
        }
    }

    private void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS player_collections (" +
                "uuid VARCHAR(36) NOT NULL," +
                "collection_id VARCHAR(64) NOT NULL," +
                "amount INTEGER DEFAULT 0," +
                "PRIMARY KEY (uuid, collection_id)" +
                ");";

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                initialize();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return connection;
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
