package com.excelutility.gui;

import net.miginfocom.swing.MigLayout;
import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

public class ModeSelectionPanel extends JPanel {

    private final AppContainer appContainer;

    public ModeSelectionPanel(AppContainer appContainer) {
        this.appContainer = appContainer;
        // Use a single MigLayout for the whole panel. `fill` fills the container, `wrap` creates a new row after each component.
        setLayout(new MigLayout("fill, wrap 1", // Layout Constraints: fill the container, add component on a new row
                "[grow, center]", // Column Constraints: one column that grows and is centered
                "[]40[]20[grow, top]40[]")); // Row Constraints: gaps between rows

        // --- Title ---
        JLabel titleLabel = new JLabel("Welcome to Excel Utility");
        titleLabel.setFont(UIConstants.FONT_HEADING);
        titleLabel.setForeground(UIConstants.COLOR_TEXT_HEADER);
        add(titleLabel, "center");

        // --- Subtitle ---
        JLabel subtitleLabel = new JLabel("Please select a mode to begin");
        subtitleLabel.setFont(UIConstants.FONT_BODY);
        subtitleLabel.setForeground(UIConstants.COLOR_TEXT_BODY);
        add(subtitleLabel, "center");

        // --- Center Panel for Mode Buttons ---
        JPanel centerPanel = new JPanel(new MigLayout("fillx, nogrid, insets 0", "[grow, sg btn]20[grow, sg btn]")); // sg makes columns have the same size
        centerPanel.setBackground(this.getBackground());

        JPanel comparePanel = createModePanel(
            "Compare Excel Files",
            "Perform a detailed, cell-by-cell comparison of two Excel sheets. Identify differences and find records that exist in one file but not the other.",
            e -> appContainer.navigateTo("compare")
        );

        JPanel filterPanel = createModePanel(
            "Spec QA Recon",
            "Filter a data sheet using values from another file. Build complex, nested AND/OR logic and export the final dataset.",
            e -> appContainer.navigateTo("filter")
        );

        centerPanel.add(comparePanel, "grow");
        centerPanel.add(filterPanel, "grow");
        add(centerPanel, "growx");

        // --- Footer ---
        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5));
        footerPanel.add(createFooterLabel("Version: 1.1.0"));
        footerPanel.add(createFooterSeparator());
        footerPanel.add(createFooterLabel("Getting Started")); // Removed pseudo-HTML
        footerPanel.add(createFooterSeparator());
        footerPanel.add(createFooterLabel("View Logs"));
        add(footerPanel, "center, gaptop 20");
    }

    private JPanel createModePanel(String title, String description, java.awt.event.ActionListener action) {
        JPanel panel = new JPanel(new MigLayout("wrap 1, fill", "[grow]", "[][grow]"));
        panel.setBorder(UIConstants.BORDER_PANEL);
        panel.setBackground(Color.WHITE);

        JButton titleButton = new JButton(title);
        titleButton.setFont(UIConstants.FONT_SUBHEADING);
        titleButton.addActionListener(action);
        // A more professional button look
        titleButton.setFocusPainted(false);
        titleButton.setBackground(UIConstants.COLOR_PRIMARY_BUTTON);
        titleButton.setForeground(Color.WHITE);
        titleButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        panel.add(titleButton, "growx, h 60!");

        // Use a JLabel with HTML for clean, wrapped text.
        String htmlDescription = "<html><body style='width: 250px; font-family: SansSerif; font-size: 11pt; color: #505050;'>"
                               + description
                               + "</body></html>";
        JLabel descLabel = new JLabel(htmlDescription);
        descLabel.setVerticalAlignment(SwingConstants.TOP);

        // Add padding to the description
        JPanel descriptionWrapper = new JPanel(new BorderLayout());
        descriptionWrapper.setBorder(UIConstants.BORDER_EMPTY_10);
        descriptionWrapper.setBackground(Color.WHITE);
        descriptionWrapper.add(descLabel, BorderLayout.CENTER);

        panel.add(descriptionWrapper, "grow");

        return panel;
    }

    private JLabel createFooterLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(UIConstants.FONT_FOOTER);
        label.setForeground(UIConstants.COLOR_TEXT_BODY);
        return label;
    }

    private JSeparator createFooterSeparator() {
        JSeparator separator = new JSeparator(SwingConstants.VERTICAL);
        separator.setPreferredSize(new Dimension(1, 15));
        separator.setForeground(UIConstants.COLOR_BORDER);
        return separator;
    }
}
