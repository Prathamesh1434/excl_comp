package com.excelutility.gui;

import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;

public class ModeSelectionPanel extends JPanel {

    private AppContainer appContainer;

    public ModeSelectionPanel(AppContainer appContainer) {
        this.appContainer = appContainer;
        setLayout(new MigLayout("align 50% 50%"));

        JLabel titleLabel = new JLabel("Select a Mode");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        add(titleLabel, "wrap, center, gaptop 20");

        JButton compareButton = new JButton("Compare Excel Files");
        compareButton.setPreferredSize(new Dimension(200, 50));
        JButton filterButton = new JButton("Filter Excel Data");
        filterButton.setPreferredSize(new Dimension(200, 50));

        compareButton.addActionListener(e -> appContainer.navigateTo("compare"));
        filterButton.addActionListener(e -> appContainer.navigateTo("filter"));

        add(compareButton, "gaptop 20");
        add(filterButton, "gaptop 20");
    }
}
