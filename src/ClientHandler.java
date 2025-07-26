package src;

import java.util.List;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import src.model.DocumentVersion;
import src.service.DocumentService;

class ClientHandler implements Runnable {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String currentDocument = null;
    private String username = null;
    private boolean connected = true;
    
    public ClientHandler(Socket socket) {
        this.socket = socket;
        try {
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            System.err.println("Error setting up client: " + e.getMessage());
            connected = false;
        }
    }
    
    @Override
    public void run() {
        try {
            // First message should be the username
            username = in.readLine();
            if (username == null) {
                connected = false;
                return;
            }
            
            System.out.println("User connected: " + username);
            
            // Send list of available documents
            sendDocumentList();
            
            String message;
            while (connected && (message = in.readLine()) != null) {
                System.out.println("Received from " + username + ": " + message);
                
                if (message.startsWith("JOIN:")) {
                    handleJoinDocument(message.substring(5));
                } 
                else if (message.startsWith("TEXT:") && currentDocument != null) {
                    handleDocumentUpdate(message.substring(5));
                }
                else if (message.startsWith("CURSOR:") && currentDocument != null) {
                    handleCursorUpdate(message.substring(7));
                }
                else if (message.equals("LIST")) {
                    sendDocumentList();
                }
                else if (message.startsWith("GET_VERSIONS:")) {
                    handleGetVersions(message.substring(13));
                }
                else if (message.startsWith("GET_VERSION:")) {
                    handleGetVersion(message.substring(12));
                }
            }
        } catch (IOException e) {
            System.err.println("Client communication error: " + e.getMessage());
        } finally {
            cleanup();
        }
    }
    
    private void handleJoinDocument(String documentName) {
        System.out.println("User " + username + " joining document: " + documentName);
        
        if (currentDocument != null) {
            CollabServer.leaveDocument(currentDocument, this);
        }
        currentDocument = documentName;
        CollabServer.joinDocument(documentName, this);
    }
    
    private void handleDocumentUpdate(String content) {
        CollabServer.updateDocument(currentDocument, content, this);
    }
    
    private void handleCursorUpdate(String positionStr) {
        try {
            int position = Integer.parseInt(positionStr);
            CollabServer.updateCursorPosition(currentDocument, username, position);
        } catch (NumberFormatException e) {
            System.err.println("Invalid cursor position: " + positionStr);
        }
    }
    
    private void handleGetVersions(String documentName) {
        System.out.println("User " + username + " requesting versions for document: " + documentName);
        
        List<DocumentVersion> versions = DocumentService.getDocumentVersions(documentName);
        StringBuilder sb = new StringBuilder("VERSIONS:" + documentName + ":");
        
        for (DocumentVersion version : versions) {
            sb.append(version.getId()).append(",")
              .append(version.getCreatedAt()).append(",")
              .append(version.getCreatedBy()).append(";");
        }
        
        System.out.println("Sending versions response: " + sb.toString());
        out.println(sb.toString());
    }
    
    private void handleGetVersion(String versionIdStr) {
        try {
            int versionId = Integer.parseInt(versionIdStr);
            System.out.println("User " + username + " requesting version: " + versionId);
            
            DocumentVersion version = DocumentService.getVersionById(versionId);
            if (version != null) {
                System.out.println("Sending version content, length: " + version.getContent().length());
                out.println("VERSION_CONTENT:" + version.getContent());
            } else {
                System.err.println("Version not found: " + versionIdStr);
            }
        } catch (NumberFormatException e) {
            System.err.println("Invalid version ID: " + versionIdStr);
        }
    }
    
    public void sendDocumentContent(String content) {
        if (connected && out != null) {
            out.println("DOCUMENT:" + content);
        }
    }
    
    public void sendDocumentUpdate(String content) {
        if (connected && out != null) {
            out.println("UPDATE:" + content);
        }
    }
    
    public void sendCursorPosition(String username, int position) {
        if (connected && out != null) {
            out.println("CURSOR_POS:" + username + ":" + position);
        }
    }
    
    public void sendActiveUsers(String documentName, List<String> users) {
        if (connected && out != null) {
            out.println("ACTIVE_USERS:" + documentName + ":" + String.join(",", users));
        }
    }
    
    public void sendDocumentList() {
        if (connected && out != null) {
            List<String> documents = DocumentService.getAllDocuments();
            StringBuilder sb = new StringBuilder("LIST:");
            for (String doc : documents) {
                sb.append(doc).append(",");
            }
            out.println(sb.toString());
        }
    }
    
    public String getUsername() {
        return username;
    }
    
    private void cleanup() {
        connected = false;
        
        if (currentDocument != null) {
            CollabServer.leaveDocument(currentDocument, this);
        }
        
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            System.err.println("Error closing client connection: " + e.getMessage());
        }
    }
}
