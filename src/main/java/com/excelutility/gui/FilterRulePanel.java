package com.excelutility.gui;

import com.excelutility.core.AutoNamingService;
import com.excelutility.core.FilterRule;
import com.excelutility.core.expression.FilterExpression;
import com.excelutility.core.expression.RuleNode;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * A panel that displays a single filter rule and provides actions for it.
 * This has been updated for the new GUI design.
 */
public class FilterRulePanel extends JPanel implements ExpressionNodeComponent {

    private final FilterRule rule;
    private final JTextField ruleNameField;
    private final JLabel recordCountLabel;
    private final JButton previewButton;

    /**
     * Constructs a panel for a given filter rule.
     *
     * @param rule           The {@link FilterRule} to display.
     * @param panelProvider  A reference to the main FilterPanel to call back to for actions.
     * @param deleteListener The {@link ActionListener} to be invoked when the delete button is clicked.
     */
    public FilterRulePanel(FilterRule rule, FilterPanel panelProvider, ActionListener deleteListener) {
        this.rule = rule;
        // Using a more detailed layout for alignment
        setLayout(new MigLayout("insets 5, fillx", "[grow]rel[auto]rel[auto]rel[auto]"));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(2, 5, 2, 5)
        ));
        setBackground(Color.WHITE);

        ruleNameField = new JTextField(AutoNamingService.suggestRuleName());
        add(ruleNameField, "growx, wmin 100");

        JLabel ruleLabel = new JLabel(rule.getDescriptiveName());
        ruleLabel.setForeground(Color.DARK_GRAY);
        add(ruleLabel, "growx, gapleft 10");

        recordCountLabel = new JLabel("(N/A)");
        recordCountLabel.setFont(recordCountLabel.getFont().deriveFont(Font.BOLD));
        add(recordCountLabel, "gapleft 10");

        previewButton = new JButton("Preview");
        previewButton.setToolTipText("Preview matching results for this rule in a new tab");
        previewButton.addActionListener(e -> panelProvider.previewRule(this));
        add(previewButton);

        JButton deleteButton = new JButton("X");
        deleteButton.setToolTipText("Delete this filter rule");
        deleteButton.addActionListener(e -> deleteListener.actionPerformed(
                new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null)
        ));
        add(deleteButton);
    }

    public String getRuleName() {
        return ruleNameField.getText();
    }

    public FilterRule getRule() {
        return rule;
    }

    public void setRecordCount(int count) {
        recordCountLabel.setText("(" + count + ")");
        if (count == 0) {
            recordCountLabel.setForeground(Color.RED);
        } else {
            recordCountLabel.setForeground(new Color(0, 153, 0)); // Dark Green
        }
    }

    @Override
    public FilterExpression getExpression() {
        // The RuleNode currently doesn't store the name, but the expression tree
        // doesn't need it for evaluation. The name is for UI purposes.
        return new RuleNode(this.rule);
    }
}
