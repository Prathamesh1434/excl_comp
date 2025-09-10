package com.excelutility;

import com.excelutility.gui.MainFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * The main entry point for the Excel Utility application.
 */
public class App {
    /**
     * The main method that launches the application.
     *
     * @param args Command line arguments (not used).
     */
    public static void main(String[] args) {
        // Set a modern look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            MainFrame mainFrame = new MainFrame();
            mainFrame.setVisible(true);
        });
    }
}
