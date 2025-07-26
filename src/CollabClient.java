package src;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class CollabClient {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private MessageListener listener;
    private boolean connected = false;
    private String currentDocument = null;
    private String username;
    private AtomicInteger lastCursorPosition = new AtomicInteger(0);
    
    public interface MessageListener {
        void onDocumentReceived(String content);
        void onDocumentUpdated(String content);
        void onDocumentListReceived(List<String> documents);
        void onConnectionStatusChanged(boolean connected);
        void onUserJoined(String documentName, String username);
        void onUserLeft(String documentName, String username);
        void onActiveUsersUpdated(String documentName, List<String> users);
        void onCursorPositionChanged(String username, int position);
        void onDocumentVersionsReceived(String documentName, List<VersionInfo> versions);
        void onVersionContentReceived(String content);
    }
    
    public static class VersionInfo {
        public final int id;
        public final String timestamp;
        public final String author;
        
        public VersionInfo(int id, String timestamp, String author) {
            this.id = id;
            this.timestamp = timestamp;
            this.author = author;
        }
        
        @Override
        public String toString() {
            return "Version " + id + " by " + author + " at " + timestamp;
        }
    }
    
    public CollabClient(String host, int port, String username, MessageListener listener) {
        this.username = username;
        this.listener = listener;
        connect(host, port);
    }
    
    private void connect(String host, int port) {
        try {
            socket = new Socket(host, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            // Send username first
            out.println(username);
            
            connected = true;
            
            if (listener != null) {
                listener.onConnectionStatusChanged(true);
            }
            
            // Start listening for messages
            new Thread(this::listenForMessages).start();
            
            System.out.println("Connected to server at " + host + ":" + port);
        } catch (IOException e) {
            System.err.println("Failed to connect to server: " + e.getMessage());
            connected = false;
            if (listener != null) {
                listener.onConnectionStatusChanged(false);
            }
        }
    }
    
    private void listenForMessages() {
        try {
            String message;
            while (connected && (message = in.readLine()) != null) {
                System.out.println("Client received: " + message);
                
                if (message.startsWith("DOCUMENT:")) {
                    handleDocumentMessage(message.substring(9));
                } 
                else if (message.startsWith("UPDATE:")) {
                    handleUpdateMessage(message.substring(7));
                }
                else if (message.startsWith("LIST:")) {
                    handleListMessage(message.substring(5));
                }
                else if (message.startsWith("USER_JOINED:")) {
                    handleUserJoinedMessage(message.substring(12));
                }
                else if (message.startsWith("USER_LEFT:")) {
                    handleUserLeftMessage(message.substring(10));
                }
                else if (message.startsWith("ACTIVE_USERS:")) {
                    handleActiveUsersMessage(message.substring(13));
                }
                else if (message.startsWith("CURSOR_POS:")) {
                    handleCursorPositionMessage(message.substring(11));
                }
                else if (message.startsWith("VERSIONS:")) {
                    handleVersionsMessage(message.substring(9));
                }
                else if (message.startsWith("VERSION_CONTENT:")) {
                    handleVersionContentMessage(message.substring(16));
                }
            }
        } catch (IOException e) {
            System.err.println("Connection lost: " + e.getMessage());
        } finally {
            disconnect();
        }
    }
    
    private void handleDocumentMessage(String content) {
        if (listener != null) {
            listener.onDocumentReceived(content);
        }
    }
    
    private void handleUpdateMessage(String content) {
        if (listener != null) {
            listener.onDocumentUpdated(content);
        }
    }
    
    private void handleListMessage(String listStr) {
        if (listener != null) {
            String[] docArray = listStr.split(",");
            List<String> documents = new ArrayList<>();
            for (String doc : docArray) {
                if (!doc.isEmpty()) {
                    documents.add(doc);
                }
            }
            listener.onDocumentListReceived(documents);
        }
    }
    
    private void handleUserJoinedMessage(String message) {
        String[] parts = message.split(":");
        if (parts.length == 2 && listener != null) {
            listener.onUserJoined(parts[0], parts[1]);
        }
    }
    
    private void handleUserLeftMessage(String message) {
        String[] parts = message.split(":");
        if (parts.length == 2 && listener != null) {
            listener.onUserLeft(parts[0], parts[1]);
        }
    }
    
    private void handleActiveUsersMessage(String message) {
        String[] parts = message.split(":");
        if (parts.length == 2 && listener != null) {
            List<String> users = Arrays.asList(parts[1].split(","));
            listener.onActiveUsersUpdated(parts[0], users);
        }
    }
    
    private void handleCursorPositionMessage(String message) {
        String[] parts = message.split(":");
        if (parts.length == 2 && listener != null) {
            try {
                int position = Integer.parseInt(parts[1]);
                listener.onCursorPositionChanged(parts[0], position);
            } catch (NumberFormatException e) {
                System.err.println("Invalid cursor position: " + parts[1]);
            }
        }
    }
    
    private void handleVersionsMessage(String message) {
        System.out.println("Processing versions message: " + message);
        String[] parts = message.split(":", 2);
        if (parts.length == 2 && listener != null) {
            String documentName = parts[0];
            List<VersionInfo> versions = new ArrayList<>();
            
            String[] versionEntries = parts[1].split(";");
            for (String entry : versionEntries) {
                if (!entry.isEmpty()) {
                    String[] fields = entry.split(",");
                    if (fields.length == 3) {
                        try {
                            int id = Integer.parseInt(fields[0]);
                            versions.add(new VersionInfo(id, fields[1], fields[2]));
                            System.out.println("Added version: " + id + " by " + fields[2]);
                        } catch (NumberFormatException e) {
                            System.err.println("Invalid version ID: " + fields[0]);
                        }
                    }
                }
            }
            
            System.out.println("Notifying listener with " + versions.size() + " versions");
            listener.onDocumentVersionsReceived(documentName, versions);
        } else {
            System.err.println("Invalid versions message format: " + message);
        }
    }
    
    private void handleVersionContentMessage(String content) {
        if (listener != null) {
            listener.onVersionContentReceived(content);
        }
    }
    
    public void joinDocument(String documentName) {
        if (connected && out != null) {
            currentDocument = documentName;
            out.println("JOIN:" + documentName);
            
            // Request versions for this document
            requestDocumentVersions(documentName);
        }
    }
    
    public void sendText(String text) {
        if (connected && out != null && currentDocument != null) {
            out.println("TEXT:" + text);
        }
    }
    
    public void sendCursorPosition(int position) {
        if (connected && out != null && currentDocument != null && position != lastCursorPosition.get()) {
            lastCursorPosition.set(position);
            out.println("CURSOR:" + position);
        }
    }
    
    public void requestDocumentList() {
        if (connected && out != null) {
            out.println("LIST");
        }
    }
    
    public void requestDocumentVersions(String documentName) {
        if (connected && out != null) {
            System.out.println("Requesting versions for document: " + documentName);
            out.println("GET_VERSIONS:" + documentName);
        }
    }
    
    public void requestVersionContent(int versionId) {
        if (connected && out != null) {
            System.out.println("Requesting content for version: " + versionId);
            out.println("GET_VERSION:" + versionId);
        }
    }
    
    public void disconnect() {
        connected = false;
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            System.err.println("Error disconnecting: " + e.getMessage());
        }
        
        if (listener != null) {
            listener.onConnectionStatusChanged(false);
        }
    }
    
    public boolean isConnected() {
        return connected;
    }
    
    public String getCurrentDocument() {
        return currentDocument;
    }
}
