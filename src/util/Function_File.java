package src.util;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.*;

public class Function_File {
    
    private Component parent;
    private JTextArea textArea;
    private boolean hasChanges = false;
    private String currentFileName = null;
    private String currentFilePath = null;
    
    // Green theme colors
    private static final Color PRIMARY_GREEN = new Color(46, 125, 50);
    
    public Function_File(Component parent, JTextArea textArea) {
        this.parent = parent;
        this.textArea = textArea;
    }
    
    public void newFile() {
        if (hasChanges) {
            int choice = JOptionPane.showConfirmDialog(
                parent,
                "Do you want to save changes to the current document?",
                "Save Changes",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE
            );
            
            if (choice == JOptionPane.YES_OPTION) {
                if (!save()) {
                    return; // User cancelled save
                }
            } else if (choice == JOptionPane.CANCEL_OPTION) {
                return; // User cancelled new file
            }
        }
        
        textArea.setText("");
        currentFileName = null;
        currentFilePath = null;
        hasChanges = false;
        updateTitle();
    }
    
    public void open() {
        if (hasChanges) {
            int choice = JOptionPane.showConfirmDialog(
                parent,
                "Do you want to save changes to the current document?",
                "Save Changes",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE
            );
            
            if (choice == JOptionPane.YES_OPTION) {
                if (!save()) {
                    return; // User cancelled save
                }
            } else if (choice == JOptionPane.CANCEL_OPTION) {
                return; // User cancelled open
            }
        }
        
        JFileChooser fileChooser = createStyledFileChooser();
        fileChooser.setDialogTitle("Open Document");
        
        int result = fileChooser.showOpenDialog(parent);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                StringBuilder content = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        content.append(line).append("\n");
                    }
                }
                
                textArea.setText(content.toString());
                currentFileName = file.getName();
                currentFilePath = file.getAbsolutePath();
                hasChanges = false;
                updateTitle();
                
                JOptionPane.showMessageDialog(
                    parent,
                    "File opened successfully!",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE
                );
                
            } catch (IOException e) {
                JOptionPane.showMessageDialog(
                    parent,
                    "Error opening file: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }
    
    public boolean save() {
        if (currentFilePath != null) {
            return saveToFile(new File(currentFilePath));
        } else {
            return saveAs();
        }
    }
    
    public boolean saveAs() {
        JFileChooser fileChooser = createStyledFileChooser();
        fileChooser.setDialogTitle("Save Document As");
        
        if (currentFileName != null) {
            fileChooser.setSelectedFile(new File(currentFileName));
        }
        
        int result = fileChooser.showSaveDialog(parent);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            
            // Add .txt extension if no extension provided
            if (!file.getName().contains(".")) {
                file = new File(file.getAbsolutePath() + ".txt");
            }
            
            // Check if file exists
            if (file.exists()) {
                int choice = JOptionPane.showConfirmDialog(
                    parent,
                    "File already exists. Do you want to overwrite it?",
                    "File Exists",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
                );
                
                if (choice != JOptionPane.YES_OPTION) {
                    return false;
                }
            }
            
            return saveToFile(file);
        }
        
        return false;
    }
    
    private boolean saveToFile(File file) {
        try {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                writer.write(textArea.getText());
            }
            
            currentFileName = file.getName();
            currentFilePath = file.getAbsolutePath();
            hasChanges = false;
            updateTitle();
            
            JOptionPane.showMessageDialog(
                parent,
                "File saved successfully!",
                "Success",
                JOptionPane.INFORMATION_MESSAGE
            );
            
            return true;
            
        } catch (IOException e) {
            JOptionPane.showMessageDialog(
                parent,
                "Error saving file: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE
            );
            return false;
        }
    }
    
    public void exit() {
        if (hasChanges) {
            int choice = JOptionPane.showConfirmDialog(
                parent,
                "Do you want to save changes before exiting?",
                "Save Changes",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE
            );
            
            if (choice == JOptionPane.YES_OPTION) {
                if (!save()) {
                    return; // User cancelled save
                }
            } else if (choice == JOptionPane.CANCEL_OPTION) {
                return; // User cancelled exit
            }
        }
        
        System.exit(0);
    }
    
    private JFileChooser createStyledFileChooser() {
        JFileChooser fileChooser = new JFileChooser();
        
        // Set file filters
        FileNameExtensionFilter txtFilter = new FileNameExtensionFilter("Text Files (*.txt)", "txt");
        FileNameExtensionFilter allFilter = new FileNameExtensionFilter("All Files (*.*)", "*");
        
        fileChooser.addChoosableFileFilter(txtFilter);
        fileChooser.addChoosableFileFilter(allFilter);
        fileChooser.setFileFilter(txtFilter);
        
        // Style the file chooser
        try {
            UIManager.put("FileChooser.background", Color.WHITE);
            UIManager.put("FileChooser.foreground", PRIMARY_GREEN);
        } catch (Exception e) {
            // Ignore styling errors
        }
        
        return fileChooser;
    }
    
    private void updateTitle() {
        if (parent instanceof JFrame) {
            JFrame frame = (JFrame) parent;
            String title = "Collaborative Notepad";
            if (currentFileName != null) {
                title += " - " + currentFileName;
                if (hasChanges) {
                    title += " *";
                }
            }
            frame.setTitle(title);
        }
    }
    
    public void setHasChanges(boolean hasChanges) {
        this.hasChanges = hasChanges;
        updateTitle();
    }
    
    public boolean hasChanges() {
        return hasChanges;
    }
    
    public String getCurrentFileName() {
        return currentFileName;
    }
    
    public String getCurrentFilePath() {
        return currentFilePath;
    }
}

