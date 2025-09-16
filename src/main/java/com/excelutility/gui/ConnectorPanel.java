package com.excelutility.gui;

import com.excelutility.core.FilteringService;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;

/**
 * A simple panel that displays AND/OR radio buttons to connect two filter components.
 */
public class ConnectorPanel extends JPanel {

    private final JRadioButton andButton;
    private final JRadioButton orButton;

    public ConnectorPanel() {
        setLayout(new MigLayout("insets 0, align center"));
        setBorder(BorderFactory.createEmptyBorder(5, 20, 5, 20));

        andButton = new JRadioButton("AND");
        andButton.setSelected(true); // Default to AND
        orButton = new JRadioButton("OR");

        ButtonGroup group = new ButtonGroup();
        group.add(andButton);
        group.add(orButton);

        JSeparator separator = new JSeparator();
        add(separator, "growx, wrap, span");

        add(andButton, "split 2");
        add(orButton);

        JSeparator separator2 = new JSeparator();
        add(separator2, "growx, wrap, span, gaptop 5");
    }

    /**
     * Gets the logical operator selected in this connector panel.
     *
     * @return The selected {@link FilteringService.LogicalOperator}.
     */
    public FilteringService.LogicalOperator getOperator() {
        return andButton.isSelected() ? FilteringService.LogicalOperator.AND : FilteringService.LogicalOperator.OR;
    }

    /**
     * Sets the selected logical operator for this panel.
     *
     * @param operator The {@link FilteringService.LogicalOperator} to select.
     */
    public void setOperator(FilteringService.LogicalOperator operator) {
        if (operator == FilteringService.LogicalOperator.AND) {
            andButton.setSelected(true);
        } else {
            orButton.setSelected(true);
        }
    }
}
