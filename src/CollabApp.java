package src;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class CollabApp extends JFrame {
    private NotepadComponent notepad;
    private String username;
    
    public CollabApp() {
        initializeFrame();
        
        // Show login dialog
        LoginDialog.LoginResult result = LoginDialog.showLoginDialog(this);
        if (result.isAuthenticated()) {
            this.username = result.getUsername();
            createNotepad();
            setVisible(true);
        } else {
            System.exit(0);
        }
    }
    
    private void initializeFrame() {
        setTitle("Collaborative Notepad");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);
        
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (notepad != null) {
                    notepad.cleanup();
                }
            }
        });
    }
    
    private void createNotepad() {
        notepad = new NotepadComponent("localhost", 5000, username);
        setJMenuBar(notepad.getMenuBar());
        add(notepad, BorderLayout.CENTER);
        setTitle("Collaborative Notepad - " + username);
    }
    
    public static void main(String[] args) {
        // Initialize database
        src.util.DBSetup.createTables();
        
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                
            }
            
            new CollabApp();
        });
    }
}