package com.excelutility;

import com.excelutility.gui.MainFrame;
import javax.swing.SwingUtilities;

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
        SwingUtilities.invokeLater(() -> {
            MainFrame mainFrame = new MainFrame();
            mainFrame.setVisible(true);
        });
    }
}
