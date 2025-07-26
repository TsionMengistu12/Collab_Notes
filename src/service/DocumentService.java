package src.service;

import src.util.DBUtil;
import src.model.DocumentVersion;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DocumentService {
    
    public static boolean saveDocument(String name, String content) {
        String sql = "INSERT OR REPLACE INTO documents (name, content, updated_at) VALUES (?, ?, CURRENT_TIMESTAMP)";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, name);
            pstmt.setString(2, content);
            
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("Error saving document: " + e.getMessage());
            return false;
        }
    }
    
    public static String loadDocument(String name) {
        String sql = "SELECT content FROM documents WHERE name = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, name);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("content");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error loading document: " + e.getMessage());
        }
        
        return "";
    }
    
    public static List<String> getAllDocuments() {
        List<String> documents = new ArrayList<>();
        String sql = "SELECT name FROM documents ORDER BY updated_at DESC";
        
        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                documents.add(rs.getString("name"));
            }
        } catch (SQLException e) {
            System.err.println("Error getting documents: " + e.getMessage());
        }
        
        return documents;
    }
    
    public static boolean documentExists(String name) {
        String sql = "SELECT 1 FROM documents WHERE name = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, name);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("Error checking if document exists: " + e.getMessage());
            return false;
        }
    }
    
    public static boolean saveVersion(String documentName, String content, String username) {
        System.out.println("Saving version for document: " + documentName + " by user: " + username);
        String sql = "INSERT INTO document_versions (document_name, content, created_by) VALUES (?, ?, ?)";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, documentName);
            pstmt.setString(2, content);
            pstmt.setString(3, username);
            
            int result = pstmt.executeUpdate();
            System.out.println("Version saved successfully. Result: " + result);
            return true;
        } catch (SQLException e) {
            System.err.println("Error saving document version: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    public static List<DocumentVersion> getDocumentVersions(String documentName) {
        System.out.println("Getting versions for document: " + documentName);
        List<DocumentVersion> versions = new ArrayList<>();
        String sql = "SELECT id, content, created_at, created_by FROM document_versions " +
                    "WHERE document_name = ? ORDER BY created_at DESC";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, documentName);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    DocumentVersion version = new DocumentVersion(
                        rs.getInt("id"),
                        documentName,
                        rs.getString("content"),
                        rs.getTimestamp("created_at"),
                        rs.getString("created_by")
                    );
                    versions.add(version);
                    System.out.println("Found version: " + version.getId() + " by " + version.getCreatedBy());
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting document versions: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("Total versions found: " + versions.size());
        return versions;
    }

    public static void updateUserPresence(String documentName, String username) {
        String sql = "INSERT OR REPLACE INTO active_users (document_name, username, last_active) VALUES (?, ?, CURRENT_TIMESTAMP)";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, documentName);
            pstmt.setString(2, username);
            
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error updating user presence: " + e.getMessage());
        }
    }
    
    public static void removeUserPresence(String documentName, String username) {
        String sql = "DELETE FROM active_users WHERE document_name = ? AND username = ?";
    
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, documentName);
            pstmt.setString(2, username);
            
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error removing user presence: " + e.getMessage());
        }
    }
    
    public static List<String> getActiveUsers(String documentName) {
        List<String> users = new ArrayList<>();
        String sql = "SELECT username FROM active_users WHERE document_name = ? AND last_active > datetime('now', '-2 minutes')";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, documentName);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    users.add(rs.getString("username"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting active users: " + e.getMessage());
        }
        
        return users;
    }

    public static void updateCursorPosition(String documentName, String username, int position) {
        String sql = "INSERT OR REPLACE INTO cursor_positions (document_name, username, position, last_updated) VALUES (?, ?, ?, CURRENT_TIMESTAMP)";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, documentName);
            pstmt.setString(2, username);
            pstmt.setInt(3, position);
            
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error updating cursor position: " + e.getMessage());
        }
    }

    public static Map<String, Integer> getCursorPositions(String documentName) {
        Map<String, Integer> positions = new HashMap<>();
        String sql = "SELECT username, position FROM cursor_positions WHERE document_name = ? AND last_updated > datetime('now', '-30 seconds')";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, documentName);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    positions.put(rs.getString("username"), rs.getInt("position"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting cursor positions: " + e.getMessage());
        }
        
        return positions;
    }

    public static DocumentVersion getVersionById(int versionId) {
        System.out.println("Getting version by ID: " + versionId);
        String sql = "SELECT id, document_name, content, created_at, created_by FROM document_versions WHERE id = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, versionId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    DocumentVersion version = new DocumentVersion(
                        rs.getInt("id"),
                        rs.getString("document_name"),
                        rs.getString("content"),
                        rs.getTimestamp("created_at"),
                        rs.getString("created_by")
                    );
                    System.out.println("Found version: " + version.getId() + " by " + version.getCreatedBy());
                    return version;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting version by ID: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("No version found with ID: " + versionId);
        return null;
    }
    
    // Debug method to check database tables
    public static void debugDatabaseTables() {
        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement()) {
            
            // Check document_versions table
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as count FROM document_versions")) {
                if (rs.next()) {
                    System.out.println("Total versions in database: " + rs.getInt("count"));
                }
            }
            
            // List all tables
            try (ResultSet rs = stmt.executeQuery("SELECT name FROM sqlite_master WHERE type='table'")) {
                System.out.println("Tables in database:");
                while (rs.next()) {
                    System.out.println("- " + rs.getString("name"));
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error debugging database: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
