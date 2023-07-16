package rs.etf.sab.solution;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Represents a singleton database connection.
 */
public class DB {
    // Attributes necessary to connect to a database
    private static final String USERNAME = "sa";
    private static final String PASSWORD = "123";
    private static final String DATABASE = "OnlineShop";
    private static final int PORT = 1433;
    private static final String SERVER_NAME = "localhost";

    private final Connection connection;
    private static DB db = null;

    // Connection string
    private static final String CONNECTION_STRING = "jdbc:sqlserver://" + SERVER_NAME
            + ":" + PORT
            + ";databaseName=" + DATABASE
            + ";encrypt=true;trustServerCertificate=true;";

    /**
     * Private constructor to create a new instance of the database connection.
     * Establishes a connection to the database.
     *
     * @throws RuntimeException if an SQL exception occurs during the connection process.
     */
    private DB() {
        try {
            connection = DriverManager.getConnection(CONNECTION_STRING, USERNAME, PASSWORD);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the instance of the database connection.
     * If the instance does not exist, it creates a new one and returns it.
     *
     * @return the instance of the database connection
     */
    public static DB getInstance() {
        return db == null ? (db = new DB()) : db;
    }

    /**
     * Retrieves the connection object.
     *
     * @return the connection object
     */
    public Connection getConnection() {
        return connection;
    }
}