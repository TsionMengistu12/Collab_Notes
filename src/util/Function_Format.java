package src.util;

import javax.swing.*;
import java.awt.*;

public class Function_Format {
    
    private JTextArea textArea;
    private JMenuItem wordWrapItem;
    private boolean wordWrapEnabled = true;
    
    // Available fonts
    public static final String[] AVAILABLE_FONTS = {
        "Times New Roman", "Arial", "Calibri", "Consolas", 
        "Georgia", "Verdana", "Tahoma", "Comic Sans MS"
    };
    
    // Available font sizes
    public static final Integer[] AVAILABLE_SIZES = {
        8, 9, 10, 11, 12, 14, 16, 18, 20, 22, 24, 26, 28, 36, 48, 72
    };
    
    public Function_Format(JTextArea textArea) {
        this.textArea = textArea;
    }
    
    public void toggleWordWrap() {
        wordWrapEnabled = !wordWrapEnabled;
        textArea.setLineWrap(wordWrapEnabled);
        textArea.setWrapStyleWord(wordWrapEnabled);
        
        if (wordWrapItem != null) {
            wordWrapItem.setText("Word Wrap: " + (wordWrapEnabled ? "On" : "Off"));
        }
    }
    
    public void setWordWrapMenuItem(JMenuItem item) {
        this.wordWrapItem = item;
        item.setText("Word Wrap: " + (wordWrapEnabled ? "On" : "Off"));
    }
    
    public void setFont(String fontName) {
        Font currentFont = textArea.getFont();
        Font newFont = new Font(fontName, currentFont.getStyle(), currentFont.getSize());
        textArea.setFont(newFont);
    }
    
    public void setFontSize(int size) {
        Font currentFont = textArea.getFont();
        Font newFont = new Font(currentFont.getName(), currentFont.getStyle(), size);
        textArea.setFont(newFont);
    }
    
    public void setFontStyle(int style) {
        Font currentFont = textArea.getFont();
        Font newFont = new Font(currentFont.getName(), style, currentFont.getSize());
        textArea.setFont(newFont);
    }
    
    public void setBold() {
        Font currentFont = textArea.getFont();
        int newStyle = currentFont.isBold() ? 
            currentFont.getStyle() & ~Font.BOLD : 
            currentFont.getStyle() | Font.BOLD;
        setFontStyle(newStyle);
    }
    
    public void setItalic() {
        Font currentFont = textArea.getFont();
        int newStyle = currentFont.isItalic() ? 
            currentFont.getStyle() & ~Font.ITALIC : 
            currentFont.getStyle() | Font.ITALIC;
        setFontStyle(newStyle);
    }
    
    public Font getCurrentFont() {
        return textArea.getFont();
    }
    
    public boolean isWordWrapEnabled() {
        return wordWrapEnabled;
    }
}
