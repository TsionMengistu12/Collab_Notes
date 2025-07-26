package src.util;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBUtil {

    private static final String DB_URL = "jdbc:sqlite:db/collabnote.db";

    static {
        try {
            // Ensure db directory exists
            File dbDir = new File("db");
            if (!dbDir.exists()) {
                dbDir.mkdirs();
            }
            
            // Load the SQLite JDBC driver
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println("SQLite JDBC driver not found.");
            e.printStackTrace();
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    public static void main(String[] args) {
        try (Connection conn = getConnection()) {
            if (conn != null) {
                System.out.println("Connected to SQLite database successfully.");
            }
        } catch (SQLException e) {
            System.err.println("Connection to SQLite failed.");
            e.printStackTrace();
        }
    }
}
