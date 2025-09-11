package com.excelutility.gui;

import com.excelutility.core.FilterRule;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;

public class FilterSourceDialog extends JDialog {

    private FilterRule.SourceType selectedType;
    private String selectedValue;

    public FilterSourceDialog(Frame owner, String value, String columnName) {
        super(owner, "Select Filter Source", true);
        setLayout(new MigLayout("wrap 1", "[grow]", "[]15[]15[]"));

        String displayValue = (value == null || value.trim().isEmpty()) ? "<Empty>" : value;

        JLabel infoLabel = new JLabel(String.format("<html>You selected cell with value '<b>%s</b>' in column '<b>%s</b>'.<br>How would you like to use this selection?</html>", displayValue, columnName));
        add(infoLabel, "growx");

        JButton byValueButton = new JButton(String.format("Filter by Value: '%s'", displayValue));
        JButton byColumnButton = new JButton(String.format("Filter using entire Column: '%s'", columnName));

        add(byValueButton, "growx");
        add(byColumnButton, "growx");

        byValueButton.addActionListener(e -> {
            this.selectedType = FilterRule.SourceType.BY_VALUE;
            this.selectedValue = (value == null) ? "" : value;
            setVisible(false);
        });

        byColumnButton.addActionListener(e -> {
            this.selectedType = FilterRule.SourceType.BY_COLUMN;
            this.selectedValue = columnName;
            setVisible(false);
        });

        pack();
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    }

    public FilterRule.SourceType getSelectedType() {
        return selectedType;
    }

    public String getSelectedValue() {
        return selectedValue;
    }
}
