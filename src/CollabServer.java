package src;

import src.service.DocumentService;
import src.model.DocumentVersion;
import src.util.DBSetup;
import src.util.DBUtil;

import java.io.*;
import java.net.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.concurrent.*;

public class CollabServer {
    private static final int PORT = 5000;
    private static final Map<String, Set<ClientHandler>> documentClients = new ConcurrentHashMap<>();
    private static final Map<String, String> documentContents = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    
    public static void main(String[] args) {
        System.out.println("Starting Enhanced Collaboration Server on port " + PORT);
        
        // Initialize database
        DBSetup.createTables();
        
        // Debug database tables
        DocumentService.debugDatabaseTables();
        
        // Load all documents from database
        for (String docName : DocumentService.getAllDocuments()) {
            documentContents.put(docName, DocumentService.loadDocument(docName));
        }
        
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server is running. Clients can connect to: localhost:" + PORT);
            
            // Start periodic tasks
            startDocumentSavingTask();
            startPresenceCleanupTask();
            
            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                new Thread(clientHandler).start();
                
                System.out.println("Client connected from: " + clientSocket.getInetAddress().getHostAddress());
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        } finally {
            scheduler.shutdown();
        }
    }
    
    private static void startDocumentSavingTask() {
        scheduler.scheduleAtFixedRate(() -> {
            for (Map.Entry<String, String> entry : documentContents.entrySet()) {
                DocumentService.saveDocument(entry.getKey(), entry.getValue());
                
                // Save a version if document has changed
                String dbContent = DocumentService.loadDocument(entry.getKey());
                if (!entry.getValue().equals(dbContent)) {
                    // Find a user to attribute the change to
                    Set<ClientHandler> clients = documentClients.get(entry.getKey());
                    if (clients != null && !clients.isEmpty()) {
                        String username = clients.iterator().next().getUsername();
                        DocumentService.saveVersion(entry.getKey(), entry.getValue(), username);
                    }
                }
            }
            System.out.println("All documents saved to database");
        }, 1, 1, TimeUnit.MINUTES);
    }
    
    private static void startPresenceCleanupTask() {
        scheduler.scheduleAtFixedRate(() -> {
            // Clean up users who haven't been active for more than 1 minute
            String sql = "DELETE FROM active_users WHERE last_active < datetime('now', '-1 minute')";
            try (Connection conn = DBUtil.getConnection();
                 Statement stmt = conn.createStatement()) {
                int count = stmt.executeUpdate(sql);
                if (count > 0) {
                    System.out.println("Cleaned up " + count + " inactive users");
                }
            } catch (SQLException e) {
                System.err.println("Error cleaning up presence: " + e.getMessage());
            }
        }, 1, 1, TimeUnit.MINUTES);
    }
    
    public static void joinDocument(String documentName, ClientHandler client) {
        // Add client to document's client list
        documentClients.computeIfAbsent(documentName, k -> ConcurrentHashMap.newKeySet()).add(client);
        
        // Initialize document content if it doesn't exist
        if (!documentContents.containsKey(documentName)) {
            String content = DocumentService.loadDocument(documentName);
            documentContents.put(documentName, content);
        }
        
        // Update presence
        DocumentService.updateUserPresence(documentName, client.getUsername());
        
        // Send current document content to client
        client.sendDocumentContent(documentContents.get(documentName));
        
        // Send active users list
        sendActiveUsersList(documentName);
        
        // Send cursor positions
        sendCursorPositions(documentName);
        
        System.out.println(client.getUsername() + " joined document: " + documentName + 
                         " (Total clients in document: " + documentClients.get(documentName).size() + ")");
    }
    
    public static void leaveDocument(String documentName, ClientHandler client) {
        Set<ClientHandler> clients = documentClients.get(documentName);
        if (clients != null) {
            clients.remove(client);
            
            // Update presence
            DocumentService.removeUserPresence(documentName, client.getUsername());
            
            // Notify remaining clients
            sendActiveUsersList(documentName);
            
            if (clients.isEmpty()) {
                // Save document content before removing
                String content = documentContents.get(documentName);
                DocumentService.saveDocument(documentName, content);
                
                // Remove empty document from memory (but not from database)
                documentClients.remove(documentName);
                System.out.println("Document removed from memory: " + documentName);
            }
        }
    }
    
    public static void updateDocument(String documentName, String content, ClientHandler sender) {
        // Update document content
        documentContents.put(documentName, content);
        
        // Save to database
        DocumentService.saveDocument(documentName, content);
        
        // Broadcast to all clients in the document except sender
        Set<ClientHandler> clients = documentClients.get(documentName);
        if (clients != null) {
            for (ClientHandler client : clients) {
                if (client != sender) {
                    client.sendDocumentUpdate(content);
                }
            }
        }
    }
    
    public static void updateCursorPosition(String documentName, String username, int position) {
        DocumentService.updateCursorPosition(documentName, username, position);
        
        // Broadcast to all clients in the document except the one who moved the cursor
        Set<ClientHandler> clients = documentClients.get(documentName);
        if (clients != null) {
            for (ClientHandler client : clients) {
                if (!client.getUsername().equals(username)) {
                    client.sendCursorPosition(username, position);
                }
            }
        }
    }
    
    private static void sendActiveUsersList(String documentName) {
        List<String> activeUsers = DocumentService.getActiveUsers(documentName);
        Set<ClientHandler> clients = documentClients.get(documentName);
        if (clients != null) {
            for (ClientHandler client : clients) {
                client.sendActiveUsers(documentName, activeUsers);
            }
        }
    }
    
    private static void sendCursorPositions(String documentName) {
        Map<String, Integer> positions = DocumentService.getCursorPositions(documentName);
        Set<ClientHandler> clients = documentClients.get(documentName);
        if (clients != null) {
            for (ClientHandler client : clients) {
                for (Map.Entry<String, Integer> entry : positions.entrySet()) {
                    if (!entry.getKey().equals(client.getUsername())) {
                        client.sendCursorPosition(entry.getKey(), entry.getValue());
                    }
                }
            }
        }
    }
    
    public static List<String> getDocumentList() {
        return DocumentService.getAllDocuments();
    }
    
    public static List<DocumentVersion> getDocumentVersions(String documentName) {
        return DocumentService.getDocumentVersions(documentName);
    }
    
    public static DocumentVersion getDocumentVersion(int versionId) {
        return DocumentService.getVersionById(versionId);
    }
    
    public static void saveVersion(String documentName, String content, String username) {
        boolean success = DocumentService.saveVersion(documentName, content, username);
        System.out.println("Manual version save " + (success ? "successful" : "failed") + 
                         " for document: " + documentName + " by user: " + username);
    }
}
