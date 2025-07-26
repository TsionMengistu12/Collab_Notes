package src.util;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.SQLException;

public class DBSetup {
    
    public static void createTables() {
        createUsersTable();
        createDocumentsTable();
        createVersionsTable();        
        createActiveUserTable();      
        createCursorPositionsTable(); 
    }
    
    public static void createUsersTable() {
        String sqluser = "CREATE TABLE IF NOT EXISTS users(" +
                     "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                     "username TEXT NOT NULL UNIQUE," + 
                     "password TEXT NOT NULL," +
                     "created_at TEXT DEFAULT CURRENT_TIMESTAMP" + ");";

        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sqluser);
            System.out.println(" Users table created or already exists.");
        } catch (SQLException e) {
            System.err.println(" Failed to create users table.");
            e.printStackTrace();
        }              
    }
    
    public static void createDocumentsTable() {
        String sqldoc = "CREATE TABLE IF NOT EXISTS documents(" +
                     "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                     "name TEXT NOT NULL UNIQUE," + 
                     "content TEXT DEFAULT ''," +  // Added DEFAULT
                     "created_at TEXT DEFAULT CURRENT_TIMESTAMP," +
                     "updated_at TEXT DEFAULT CURRENT_TIMESTAMP" + ");";

        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sqldoc);
            System.out.println(" Documents table created or already exists.");
            
            // Create default document if it doesn't exist
            String insertDefault = "INSERT OR IGNORE INTO documents (name, content) VALUES ('Welcome Document', 'Welcome to Collaborative Notepad!\n\nStart typing to collaborate with others in real-time.');";
            stmt.execute(insertDefault);
            System.out.println(" Default document created.");
        } catch (SQLException e) {
            System.err.println(" Failed to create documents table.");
            e.printStackTrace();
        }              
    }

    public static void createVersionsTable() {
        String sqlver = "CREATE TABLE IF NOT EXISTS document_versions (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "document_name TEXT NOT NULL," +
                        "content TEXT NOT NULL," +
                        "created_at TEXT DEFAULT CURRENT_TIMESTAMP," +  // Fixed TIMESTAMP to TEXT
                        "created_by TEXT NOT NULL," +
                        "FOREIGN KEY(document_name) REFERENCES documents(name)" + ");";

        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sqlver);
            System.out.println(" Document versions table created or already exists.");
        } catch (SQLException e) {
            System.err.println(" Failed to create document versions table.");
            e.printStackTrace();
        }           
    }

    public static void createActiveUserTable() {
        String sqlact = "CREATE TABLE IF NOT EXISTS active_users (" +
                        "document_name TEXT NOT NULL," +
                        "username TEXT NOT NULL," +
                        "last_active TEXT DEFAULT CURRENT_TIMESTAMP," +  // Fixed TIMESTAMP to TEXT
                        "PRIMARY KEY(document_name, username)," +
                        "FOREIGN KEY(document_name) REFERENCES documents(name)" + ");";

        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sqlact);
            System.out.println(" Active users table created or already exists.");
        } catch (SQLException e) {
            System.err.println(" Failed to create active users table.");
            e.printStackTrace();
        }
    }

    public static void createCursorPositionsTable() {
        String sqlcursor = "CREATE TABLE IF NOT EXISTS cursor_positions (" +
                          "document_name TEXT NOT NULL," +
                          "username TEXT NOT NULL," +
                          "position INTEGER DEFAULT 0," +
                          "last_updated TEXT DEFAULT CURRENT_TIMESTAMP," +
                          "PRIMARY KEY(document_name, username)," +
                          "FOREIGN KEY(document_name) REFERENCES documents(name)" + ");";

        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sqlcursor);
            System.out.println("Cursor positions table created or already exists.");
        } catch (SQLException e) {
            System.err.println(" Failed to create cursor positions table.");
            e.printStackTrace();
        }
    }

    // Method to verify all tables exist
    public static void verifyTables() {
        String[] tables = {"users", "documents", "document_versions", "active_users", "cursor_positions"};
        
        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement()) {
            
            System.out.println("\n Database Table Verification:");
            System.out.println("================================");
            
            for (String table : tables) {
                String sql = "SELECT name FROM sqlite_master WHERE type='table' AND name='" + table + "';";
                var rs = stmt.executeQuery(sql);
                if (rs.next()) {
                    System.out.println(" Table '" + table + "' exists");
                } else {
                    System.out.println(" Table '" + table + "' missing");
                }
                rs.close();
            }
            System.out.println("================================\n");
            
        } catch (SQLException e) {
            System.err.println(" Failed to verify tables.");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        System.out.println(" Setting up Collaborative Notepad Database...\n");
        createTables();
        verifyTables();
        System.out.println(" Database setup complete!");
    }  
}
