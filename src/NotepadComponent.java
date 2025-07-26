package src;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;

import src.service.DocumentService;
import src.util.Function_File;
import src.util.Function_Format;

import java.awt.*;
// import java.awt.event.ActionEvent;
// import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class NotepadComponent extends JPanel implements CollabClient.MessageListener {
    
    // Green theme colors
    private static final Color PRIMARY_GREEN = new Color(46, 125, 50);
    private static final Color LIGHT_GREEN = new Color(129, 199, 132);
    private static final Color DARK_GREEN = new Color(27, 94, 32);
    private static final Color ACCENT_GREEN = new Color(102, 187, 106);
    private static final Color BACKGROUND_GREEN = new Color(232, 245, 233);
    private static final Color HOVER_GREEN = new Color(165, 214, 167);
    
    private JTextArea textArea;
    private JScrollPane scrollPane;
    private JMenuBar menuBar;
    private JLabel statusLabel;
    private JComboBox<String> documentSelector;
    private CollabClient client;
    private final AtomicBoolean ignoreChanges = new AtomicBoolean(false);
    private String username;
    private AtomicLong lastChangeTime = new AtomicLong(0);
    
    // Utility classes
    private Function_File fileHandler;
    private Function_Format formatHandler;
    
    // UI components for new features
    private JPanel rightPanel;
    private JPanel userListPanel;
    private JList<String> versionList;
    private DefaultListModel<String> versionListModel;
    private Map<String, Color> userColors = new HashMap<>();
    private Map<String, Integer> cursorPositions = new HashMap<>();
    private Highlighter highlighter;
    private Map<String, Object> cursorHighlights = new HashMap<>();
    
    // Dark mode colors
    private boolean isDarkMode = false;
    private final Color LIGHT_BG = Color.WHITE;
    private final Color LIGHT_FG = Color.BLACK;
    private final Color DARK_BG = new Color(45, 45, 45);
    private final Color DARK_FG = Color.WHITE;
    
    public NotepadComponent(String host, int port, String username) {
        this.username = username;
        setLayout(new BorderLayout());
        initializeComponents();
        connectToServer(host, port);
    }
    
    private void initializeComponents() {
        // Text area with green theme
        textArea = new JTextArea();
        textArea.setFont(new Font("Times New Roman", Font.PLAIN, 14));
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        textArea.setBackground(Color.WHITE);
        textArea.setForeground(Color.BLACK);
        textArea.setCaretColor(PRIMARY_GREEN);
        textArea.setSelectionColor(LIGHT_GREEN);
        highlighter = textArea.getHighlighter();
        
        // Initialize utility classes
        fileHandler = new Function_File(this, textArea);
        formatHandler = new Function_Format(textArea);
        
        // Document listener for real-time collaboration
        textArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                handleTextChange();
                fileHandler.setHasChanges(true);
            }
            
            @Override
            public void removeUpdate(DocumentEvent e) {
                handleTextChange();
                fileHandler.setHasChanges(true);
            }
            
            @Override
            public void changedUpdate(DocumentEvent e) {
                handleTextChange();
                fileHandler.setHasChanges(true);
            }
        });
        
        // Cursor position listener
        textArea.addCaretListener(e -> {
            if (!ignoreChanges.get() && client != null && client.isConnected()) {
                client.sendCursorPosition(e.getDot());
            }
        });
        
        // Scroll pane with green theme
        scrollPane = new JScrollPane(textArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(BorderFactory.createLineBorder(LIGHT_GREEN, 2));
        scrollPane.getVerticalScrollBar().setBackground(BACKGROUND_GREEN);
        scrollPane.getHorizontalScrollBar().setBackground(BACKGROUND_GREEN);
        
        // Document selector with green theme
        documentSelector = new JComboBox<>();
        documentSelector.setEditable(true);
        documentSelector.setBackground(Color.WHITE);
        documentSelector.setForeground(DARK_GREEN);
        documentSelector.setBorder(BorderFactory.createLineBorder(LIGHT_GREEN));
        documentSelector.addActionListener(e -> {
            if (e.getActionCommand().equals("comboBoxChanged") && client != null && client.isConnected()) {
                String selectedDoc = (String) documentSelector.getSelectedItem();
                if (selectedDoc != null && !selectedDoc.isEmpty()) {
                    client.joinDocument(selectedDoc);
                }
            }
        });
        
        // Status label with green theme
        statusLabel = new JLabel("Connecting...");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        statusLabel.setBackground(BACKGROUND_GREEN);
        statusLabel.setForeground(DARK_GREEN);
        statusLabel.setOpaque(true);
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        
        // Initialize right panel with user list and version history
        initializeRightPanel();
        
        // Menu bar
        createMenuBar();
        
        // Top panel with document selector
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(BACKGROUND_GREEN);
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        
        JLabel docLabel = new JLabel("Document: ");
        docLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        docLabel.setForeground(DARK_GREEN);
        topPanel.add(docLabel, BorderLayout.WEST);
        topPanel.add(documentSelector, BorderLayout.CENTER);
        
        JButton refreshButton = new JButton("↻");
        styleGreenButton(refreshButton);
        refreshButton.setToolTipText("Refresh document list");
        refreshButton.addActionListener(e -> {
            if (client != null && client.isConnected()) {
                client.requestDocumentList();
            }
        });
        topPanel.add(refreshButton, BorderLayout.EAST);
        
        // Layout
        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(rightPanel, BorderLayout.EAST);
        add(statusLabel, BorderLayout.SOUTH);
        
        // Apply initial theme
        applyTheme();
    }
    
    private void styleGreenButton(JButton button) {
        button.setBackground(PRIMARY_GREEN);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Add hover effect
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(DARK_GREEN);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(PRIMARY_GREEN);
            }
        });
    }
    
    private void initializeRightPanel() {
        rightPanel = new JPanel(new BorderLayout());
        rightPanel.setPreferredSize(new Dimension(220, getHeight()));
        rightPanel.setBackground(BACKGROUND_GREEN);
        rightPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(LIGHT_GREEN, 2),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        
        // User list panel
        userListPanel = new JPanel();
        userListPanel.setLayout(new BoxLayout(userListPanel, BoxLayout.Y_AXIS));
        userListPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(ACCENT_GREEN),
            "Active Users",
            0, 0,
            new Font("Segoe UI", Font.BOLD, 12),
            DARK_GREEN
        ));
        userListPanel.setBackground(Color.WHITE);
        
        // Version history
        versionListModel = new DefaultListModel<>();
        versionList = new JList<>(versionListModel);
        versionList.setBackground(Color.WHITE);
        versionList.setForeground(DARK_GREEN);
        versionList.setSelectionBackground(LIGHT_GREEN);
        versionList.setSelectionForeground(DARK_GREEN);
        versionList.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        
        JScrollPane versionScrollPane = new JScrollPane(versionList);
        versionScrollPane.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(ACCENT_GREEN),
            "Version History",
            0, 0,
            new Font("Segoe UI", Font.BOLD, 12),
            DARK_GREEN
        ));
        versionScrollPane.setBackground(Color.WHITE);
        
        versionList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selected = versionList.getSelectedValue();
                if (selected != null) {
                    try {
                        int versionId = Integer.parseInt(selected.split(" - ")[0].substring(1));
                        client.requestVersionContent(versionId);
                    } catch (NumberFormatException ex) {
                        System.err.println("Error parsing version ID: " + ex.getMessage());
                    }
                }
            }
        });
        
        // Add components to right panel
        rightPanel.add(userListPanel, BorderLayout.NORTH);
        rightPanel.add(versionScrollPane, BorderLayout.CENTER);
    }
    
    private void createMenuBar() {
        menuBar = new JMenuBar();
        menuBar.setBackground(BACKGROUND_GREEN);
        menuBar.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, LIGHT_GREEN));
        
        // File menu
        JMenu fileMenu = createStyledMenu("File");
        JMenuItem newItem = createStyledMenuItem("New Document", "Ctrl+N");
        JMenuItem openItem = createStyledMenuItem("Open", "Ctrl+O");
        JMenuItem saveItem = createStyledMenuItem("Save", "Ctrl+S");
        JMenuItem saveAsItem = createStyledMenuItem("Save As", "Ctrl+Shift+S");
        JMenuItem clearItem = createStyledMenuItem("Clear All", null);
        JMenuItem saveVersionItem = createStyledMenuItem("Save Version", "Ctrl+Alt+S");
        JMenuItem exitItem = createStyledMenuItem("Exit", "Alt+F4");
        
        newItem.addActionListener(e -> {
            fileHandler.newFile();
            createNewDocument();
        });
        openItem.addActionListener(e -> fileHandler.open());
        saveItem.addActionListener(e -> fileHandler.save());
        saveAsItem.addActionListener(e -> fileHandler.saveAs());
        clearItem.addActionListener(e -> {
            if (JOptionPane.showConfirmDialog(this, 
                "Clear all text? This will affect all connected users.", 
                "Clear All", 
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                textArea.setText("");
            }
        });
        saveVersionItem.addActionListener(e -> saveCurrentVersion());
        exitItem.addActionListener(e -> fileHandler.exit());
        
        fileMenu.add(newItem);
        fileMenu.addSeparator();
        fileMenu.add(openItem);
        fileMenu.add(saveItem);
        fileMenu.add(saveAsItem);
        fileMenu.addSeparator();
        fileMenu.add(clearItem);
        fileMenu.add(saveVersionItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);
        
        // Format menu
        JMenu formatMenu = createStyledMenu("Format");
        
        // Word wrap
        JMenuItem wordWrapItem = createStyledMenuItem("Word Wrap: On", null);
        formatHandler.setWordWrapMenuItem(wordWrapItem);
        wordWrapItem.addActionListener(e -> formatHandler.toggleWordWrap());
        
        // Font submenu
        JMenu fontMenu = createStyledMenu("Font");
        for (String fontName : Function_Format.AVAILABLE_FONTS) {
            JMenuItem fontItem = createStyledMenuItem(fontName, null);
            fontItem.addActionListener(e -> formatHandler.setFont(fontName));
            fontMenu.add(fontItem);
        }
        
        // Font size submenu
        JMenu fontSizeMenu = createStyledMenu("Font Size");
        for (Integer size : Function_Format.AVAILABLE_SIZES) {
            JMenuItem sizeItem = createStyledMenuItem(size.toString(), null);
            sizeItem.addActionListener(e -> formatHandler.setFontSize(size));
            fontSizeMenu.add(sizeItem);
        }
        
        // Font style
        JMenuItem boldItem = createStyledMenuItem("Bold", "Ctrl+B");
        JMenuItem italicItem = createStyledMenuItem("Italic", "Ctrl+I");
        boldItem.addActionListener(e -> formatHandler.setBold());
        italicItem.addActionListener(e -> formatHandler.setItalic());
        
        formatMenu.add(wordWrapItem);
        formatMenu.addSeparator();
        formatMenu.add(fontMenu);
        formatMenu.add(fontSizeMenu);
        formatMenu.addSeparator();
        formatMenu.add(boldItem);
        formatMenu.add(italicItem);
        
        // View menu
        JMenu viewMenu = createStyledMenu("View");
        JMenuItem darkModeItem = createStyledMenuItem("Toggle Dark Mode", "Ctrl+D");
        JMenuItem refreshUsersItem = createStyledMenuItem("Refresh User List", "F5");
        JMenuItem zoomInItem = createStyledMenuItem("Zoom In", "Ctrl+Plus");
        JMenuItem zoomOutItem = createStyledMenuItem("Zoom Out", "Ctrl+Minus");
        
        darkModeItem.addActionListener(e -> toggleDarkMode());
        refreshUsersItem.addActionListener(e -> {
            if (client != null && client.isConnected() && client.getCurrentDocument() != null) {
                client.requestDocumentVersions(client.getCurrentDocument());
            }
        });
        zoomInItem.addActionListener(e -> {
            Font current = textArea.getFont();
            formatHandler.setFontSize(Math.min(current.getSize() + 2, 72));
        });
        zoomOutItem.addActionListener(e -> {
            Font current = textArea.getFont();
            formatHandler.setFontSize(Math.max(current.getSize() - 2, 8));
        });
        
        viewMenu.add(darkModeItem);
        viewMenu.addSeparator();
        viewMenu.add(refreshUsersItem);
        viewMenu.addSeparator();
        viewMenu.add(zoomInItem);
        viewMenu.add(zoomOutItem);
        
        
        menuBar.add(fileMenu);
        menuBar.add(formatMenu);
        menuBar.add(viewMenu);
        
    }
    
    private JMenu createStyledMenu(String text) {
        JMenu menu = new JMenu(text);
        menu.setFont(new Font("Segoe UI", Font.BOLD, 13));
        menu.setForeground(DARK_GREEN);
        menu.setBackground(BACKGROUND_GREEN);
        return menu;
    }
    
    private JMenuItem createStyledMenuItem(String text, String accelerator) {
        JMenuItem item = new JMenuItem(text);
        item.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        item.setForeground(DARK_GREEN);
        item.setBackground(Color.WHITE);
        
        if (accelerator != null) {
            item.setAccelerator(KeyStroke.getKeyStroke(accelerator));
        }
        
        // Add hover effect
        item.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                item.setBackground(HOVER_GREEN);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                item.setBackground(Color.WHITE);
            }
        });
        
        return item;
    }
    
    private void createNewDocument() {
        String docName = JOptionPane.showInputDialog(this, 
            "Enter new document name:", 
            "Create New Document", 
            JOptionPane.QUESTION_MESSAGE);
        
        if (docName != null && !docName.trim().isEmpty()) {
            boolean exists = false;
            for (int i = 0; i < documentSelector.getItemCount(); i++) {
                if (documentSelector.getItemAt(i).equals(docName)) {
                    exists = true;
                    break;
                }
            }
            
            if (!exists) {
                documentSelector.addItem(docName);
            }
            
            documentSelector.setSelectedItem(docName);
            
            if (client != null && client.isConnected()) {
                client.joinDocument(docName);
            }
        }
    }
    
    private void saveCurrentVersion() {
        if (client != null && client.isConnected() && client.getCurrentDocument() != null) {
            String content = textArea.getText();
            client.sendText(content);
            JOptionPane.showMessageDialog(this, 
                "Version saved successfully", 
                "Version Saved", 
                JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    private void connectToServer(String host, int port) {
        try {
            client = new CollabClient(host, port, username, this);
        } catch (Exception e) {
            statusLabel.setText("Failed to connect - Working offline");
            statusLabel.setForeground(Color.RED);
            JOptionPane.showMessageDialog(this, 
                "Could not connect to server at " + host + ":" + port + 
                "\nWorking in offline mode.", 
                "Connection Error", 
                JOptionPane.WARNING_MESSAGE);
        }
    }
    
    private void handleTextChange() {
        lastChangeTime.set(System.currentTimeMillis());
        
        if (!ignoreChanges.get() && client != null && client.isConnected()) {
            Timer timer = new Timer(300, e -> {
                if (!ignoreChanges.get()) {
                    client.sendText(textArea.getText());
                }
            });
            timer.setRepeats(false);
            timer.start();
        }
    }
    
    private void toggleDarkMode() {
        isDarkMode = !isDarkMode;
        applyTheme();
    }
    
    private void applyTheme() {
        if (isDarkMode) {
            textArea.setBackground(DARK_BG);
            textArea.setForeground(DARK_FG);
            textArea.setCaretColor(LIGHT_GREEN);
            textArea.setSelectionColor(DARK_GREEN);
            scrollPane.getViewport().setBackground(DARK_BG);
            setBackground(DARK_BG);
            rightPanel.setBackground(DARK_BG);
            userListPanel.setBackground(DARK_BG);
            statusLabel.setBackground(DARK_BG);
            if (!statusLabel.getForeground().equals(Color.RED)) {
                statusLabel.setForeground(LIGHT_GREEN);
            }
        } else {
            textArea.setBackground(Color.WHITE);
            textArea.setForeground(Color.BLACK);
            textArea.setCaretColor(PRIMARY_GREEN);
            textArea.setSelectionColor(LIGHT_GREEN);
            scrollPane.getViewport().setBackground(Color.WHITE);
            setBackground(BACKGROUND_GREEN);
            rightPanel.setBackground(BACKGROUND_GREEN);
            userListPanel.setBackground(Color.WHITE);
            statusLabel.setBackground(BACKGROUND_GREEN);
            if (!statusLabel.getForeground().equals(Color.RED)) {
                statusLabel.setForeground(DARK_GREEN);
            }
        }
        repaint();
    }
    
    
    private Color getUserColor(String username) {
        if (!userColors.containsKey(username)) {
            int hash = username.hashCode();
            userColors.put(username, new Color(
                Math.abs(hash % 200 + 55), 
                Math.abs((hash >> 8) % 200 + 55), 
                Math.abs((hash >> 16) % 200 + 55)
            ));
        }
        return userColors.get(username);
    }
    
    private void updateUserList(List<String> users) {
        userListPanel.removeAll();
        
        // Add current user
        JLabel currentUserLabel = new JLabel("👤 " + username + " (You)");
        currentUserLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        currentUserLabel.setForeground(PRIMARY_GREEN);
        currentUserLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        userListPanel.add(currentUserLabel);
        
        // Add other users
        for (String user : users) {
            if (!user.equals(this.username)) {
                JLabel userLabel = new JLabel("👤 " + user);
                userLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                userLabel.setForeground(getUserColor(user));
                userLabel.setBorder(BorderFactory.createEmptyBorder(3, 10, 3, 10));
                
                if (cursorPositions.containsKey(user)) {
                    int pos = cursorPositions.get(user);
                    userLabel.setText("👤 " + user + " (pos: " + pos + ")");
                }
                
                userListPanel.add(userLabel);
            }
        }
        
        userListPanel.revalidate();
        userListPanel.repaint();
    }
    
    private void updateCursorHighlights() {
        for (Object highlight : cursorHighlights.values()) {
            highlighter.removeHighlight(highlight);
        }
        cursorHighlights.clear();
        
        for (Map.Entry<String, Integer> entry : cursorPositions.entrySet()) {
            if (!entry.getKey().equals(username)) {
                try {
                    int pos = entry.getValue();
                    if (pos >= 0 && pos <= textArea.getText().length()) {
                        Color userColor = getUserColor(entry.getKey());
                        Highlighter.HighlightPainter painter = 
                            new DefaultHighlighter.DefaultHighlightPainter(
                                new Color(userColor.getRed(), userColor.getGreen(), userColor.getBlue(), 100));
                        
                        Object highlight = highlighter.addHighlight(pos, Math.min(pos + 1, textArea.getText().length()), painter);
                        cursorHighlights.put(entry.getKey(), highlight);
                    }
                } catch (Exception e) {
                    System.err.println("Error highlighting cursor position: " + e.getMessage());
                }
            }
        }
    }
    
    // MessageListener implementation
    @Override
    public void onDocumentReceived(String content) {
        SwingUtilities.invokeLater(() -> {
            ignoreChanges.set(true);
            textArea.setText(content);
            fileHandler.setHasChanges(false);
            ignoreChanges.set(false);
        });
    }
    
    @Override
    public void onDocumentUpdated(String content) {
        SwingUtilities.invokeLater(() -> {
            ignoreChanges.set(true);
            int caretPos = textArea.getCaretPosition();
            textArea.setText(content);
            if (caretPos <= content.length()) {
                textArea.setCaretPosition(caretPos);
            }
            ignoreChanges.set(false);
        });
    }
    
    @Override
    public void onDocumentListReceived(List<String> documents) {
        SwingUtilities.invokeLater(() -> {
            documentSelector.removeAllItems();
            for (String doc : documents) {
                documentSelector.addItem(doc);
            }
            
            if (client != null && client.getCurrentDocument() != null) {
                documentSelector.setSelectedItem(client.getCurrentDocument());
                client.requestDocumentVersions(client.getCurrentDocument());
            } else if (documentSelector.getItemCount() > 0) {
                documentSelector.setSelectedIndex(0);
            }
        });
    }
    
    @Override
    public void onConnectionStatusChanged(boolean connected) {
        SwingUtilities.invokeLater(() -> {
            if (connected) {
                statusLabel.setText(" Connected as " + username + " - Ready for collaboration");
                statusLabel.setForeground(PRIMARY_GREEN);
                client.requestDocumentList();
            } else {
                statusLabel.setText(" Disconnected - Working offline");
                statusLabel.setForeground(Color.RED);
            }
        });
    }
    
    @Override
    public void onUserJoined(String documentName, String username) {
        if (client != null && documentName.equals(client.getCurrentDocument())) {
            SwingUtilities.invokeLater(() -> {
                if (client != null) {
                    client.requestDocumentVersions(documentName);
                }
            });
        }
    }
    
    @Override
    public void onUserLeft(String documentName, String username) {
        if (client != null && documentName.equals(client.getCurrentDocument())) {
            SwingUtilities.invokeLater(() -> {
                cursorPositions.remove(username);
                updateCursorHighlights();
                if (client != null) {
                    client.requestDocumentVersions(documentName);
                }
            });
        }
    }
    
    @Override
    public void onActiveUsersUpdated(String documentName, List<String> users) {
        if (client != null && documentName.equals(client.getCurrentDocument())) {
            SwingUtilities.invokeLater(() -> {
                updateUserList(users);
            });
        }
    }
    
    @Override
    public void onCursorPositionChanged(String username, int position) {
        SwingUtilities.invokeLater(() -> {
            cursorPositions.put(username, position);
            updateCursorHighlights();
            if (client != null && client.getCurrentDocument() != null) {
                updateUserList(DocumentService.getActiveUsers(client.getCurrentDocument()));
            }
        });
    }
    
    @Override
    public void onDocumentVersionsReceived(String documentName, List<CollabClient.VersionInfo> versions) {
        SwingUtilities.invokeLater(() -> {
            versionListModel.clear();
            for (CollabClient.VersionInfo version : versions) {
                versionListModel.addElement(String.format(
                    "v%d - %s (%s)", 
                    version.id,
                    version.timestamp,
                    version.author
                ));
            }
        });
    }
    
    @Override
    public void onVersionContentReceived(String content) {
        SwingUtilities.invokeLater(() -> {
            int choice = JOptionPane.showConfirmDialog(this,
                "Load this version? Your current changes will be lost.",
                "Load Version",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
            
            if (choice == JOptionPane.YES_OPTION) {
                ignoreChanges.set(true);
                textArea.setText(content);
                fileHandler.setHasChanges(false);
                ignoreChanges.set(false);
            }
        });
    }
    
    public JMenuBar getMenuBar() {
        return menuBar;
    }
    
    public void cleanup() {
        if (client != null) {
            client.disconnect();
        }
    }
}
