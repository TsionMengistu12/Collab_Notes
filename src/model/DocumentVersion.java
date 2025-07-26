package src.model;

import java.sql.Timestamp;

public class DocumentVersion {
    private final int id;
    private final String documentName;
    private final String content;
    private final Timestamp createdAt;
    private final String createdBy;
    
    public DocumentVersion(int id, String documentName, String content, 
                          Timestamp createdAt, String createdBy) {
        this.id = id;
        this.documentName = documentName;
        this.content = content;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
    }
    
    // Getters
    public int getId() { 
        return id; 
    }
    
    public String getDocumentName() { 
        return documentName; 
    }
    
    public String getContent() { 
        return content; 
    }
    
    public Timestamp getCreatedAt() { 
        return createdAt; 
    }
    
    public String getCreatedBy() { 
        return createdBy; 
    }
    
    // Convenience method to get formatted timestamp
    public String getFormattedTimestamp() {
        if (createdAt != null) {
            return createdAt.toString();
        }
        return "Unknown";
    }
    
    @Override
    public String toString() {
        return String.format("Version %d - %s (%s)", 
            id, getFormattedTimestamp(), createdBy);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        DocumentVersion that = (DocumentVersion) obj;
        return id == that.id;
    }
    
    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}
