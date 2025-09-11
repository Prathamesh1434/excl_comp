package com.excelutility.gui;

import javax.swing.*;
import java.awt.*;

public class AppContainer extends JFrame {

    private CardLayout cardLayout;
    private JPanel mainPanel;
    private ModeSelectionPanel modeSelectionPanel;
    private ComparePanel comparePanel;
    private FilterPanel filterPanel;
    private JMenuBar compareMenuBar;

    public AppContainer() {
        setTitle("Excel Utility");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1600, 1000);
        setLocationRelativeTo(null);

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        modeSelectionPanel = new ModeSelectionPanel(this);
        comparePanel = new ComparePanel(this);
        filterPanel = new FilterPanel();

        // The compare panel has its own complex menu bar
        compareMenuBar = comparePanel.createMenuBar();

        mainPanel.add(modeSelectionPanel, "modeSelection");
        mainPanel.add(comparePanel, "compare");
        mainPanel.add(filterPanel, "filter");

        add(mainPanel);
        navigateTo("modeSelection"); // Start at the mode selection screen
    }

    public void navigateTo(String panelName) {
        cardLayout.show(mainPanel, panelName);
        if ("compare".equals(panelName)) {
            setJMenuBar(compareMenuBar);
        } else {
            // No menu bar for filter or mode selection panels yet
            setJMenuBar(createBaseMenuBar());
        }
        revalidate();
        repaint();
    }

    private JMenuBar createBaseMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenuItem backItem = new JMenuItem("Back to Mode Selection");
        backItem.addActionListener(e -> navigateTo("modeSelection"));
        fileMenu.add(backItem);
        menuBar.add(fileMenu);
        return menuBar;
    }
}
