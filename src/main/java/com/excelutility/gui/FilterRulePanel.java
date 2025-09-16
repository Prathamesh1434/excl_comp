package com.excelutility.gui;

import com.excelutility.core.FilterRule;
import com.excelutility.core.expression.FilterExpression;
import com.excelutility.core.expression.RuleNode;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class FilterRulePanel extends JPanel implements ExpressionNodeComponent {

    private final FilterRule rule;
    private final JLabel ruleLabel;
    private final JCheckBox selectionCheckBox;
    private final JLabel countLabel;

    public FilterRulePanel(FilterRule rule, FilterPanel panelProvider, ActionListener deleteListener) {
        this.rule = rule;
        setLayout(new MigLayout("insets 5, fillx", "[][grow]push[]"));
        setBorder(BorderFactory.createEtchedBorder());

        selectionCheckBox = new JCheckBox();
        add(selectionCheckBox);

        ruleLabel = new JLabel(rule.getDescriptiveName());
        add(ruleLabel, "growx");

        countLabel = new JLabel("(?)");
        countLabel.setOpaque(true);
        add(countLabel, "gapleft 10");

        JButton renameButton = new JButton("Rename");
        renameButton.addActionListener(e -> renameRule());

        JButton viewButton = new JButton("View");
        viewButton.addActionListener(e -> panelProvider.viewResultsForRule(rule));

        JButton downloadButton = new JButton("Download");
        downloadButton.addActionListener(e -> panelProvider.downloadResultsForRule(rule));

        JButton deleteButton = new JButton("X");
        deleteButton.addActionListener(e -> deleteListener.actionPerformed(
                new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null)
        ));

        add(renameButton, "split 4, gapleft 20");
        add(viewButton);
        add(downloadButton);
        add(deleteButton);
    }

    public void updateCount(int count) {
        if (count < 0) {
            countLabel.setText("(Error)");
            countLabel.setBackground(Color.ORANGE);
        } else {
            countLabel.setText("(" + count + ")");
            countLabel.setBackground(count > 0 ? new Color(220, 255, 220) : new Color(255, 220, 220));
        }
    }

    private void renameRule() {
        String newName = JOptionPane.showInputDialog(this, "Enter new name for the rule:", rule.getDescriptiveName());
        if (newName != null && !newName.trim().isEmpty()) {
            // A proper rename would require creating a new FilterRule object and replacing this panel.
            // For now, just update the label.
            ruleLabel.setText(newName);
        }
    }

    @Override
    public FilterExpression getExpression() {
        return new RuleNode(this.rule);
    }

    @Override
    public boolean isSelected() {
        return selectionCheckBox.isSelected();
    }

    @Override
    public Component getComponent() {
        return this;
    }
}
