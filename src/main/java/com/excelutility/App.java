package com.excelutility;

import com.excelutility.gui.FilterFrame;
import com.excelutility.gui.MainFrame;
import com.excelutility.gui.StartupModeDialog;
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
        // Set a cross-platform look and feel for consistency
        try {
            // UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // If the native L&F fails, the default (Metal) will be used.
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            StartupModeDialog startupDialog = new StartupModeDialog(null);
            startupDialog.setVisible(true);

            StartupModeDialog.Mode selectedMode = startupDialog.getSelectedMode();

            switch (selectedMode) {
                case COMPARE:
                    MainFrame mainFrame = new MainFrame();
                    mainFrame.setVisible(true);
                    break;
                case FILTER:
                    FilterFrame filterFrame = new FilterFrame();
                    filterFrame.setVisible(true);
                    break;
                case NONE:
                default:
                    // User closed the dialog, so exit the application
                    System.exit(0);
                    break;
            }
        });
    }
}
