package com.excelutility.gui;

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
 */
public class FilterRulePanel extends JPanel implements ExpressionNodeComponent {

    private final FilterRule rule;
    private final JLabel countLabel;

    /**
     * Constructs a panel for a given filter rule.
     *
     * @param rule           The {@link FilterRule} to display.
     * @param panelProvider  A reference to the main FilterPanel to call back to for actions.
     * @param deleteListener The {@link ActionListener} to be invoked when the delete button is clicked.
     */
    public FilterRulePanel(FilterRule rule, FilterPanel panelProvider, ActionListener deleteListener) {
        this.rule = rule;
        setLayout(new MigLayout("insets 5, fillx", "[grow]push[][][]"));
        setBorder(BorderFactory.createEtchedBorder());

        JLabel ruleLabel = new JLabel(rule.getDescriptiveName());
        add(ruleLabel, "growx");

        countLabel = new JLabel("(?)");
        countLabel.setOpaque(true);
        add(countLabel, "gapleft 10");

        JButton viewButton = new JButton("View");
        viewButton.addActionListener(e -> panelProvider.viewResultsForRule(rule));
        add(viewButton);

        JButton deleteButton = new JButton("X");
        deleteButton.setToolTipText("Delete this filter rule");
        deleteButton.addActionListener(e -> deleteListener.actionPerformed(
                new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null)
        ));
        add(deleteButton);
    }

    @Override
    public FilterExpression getExpression() {
        return new RuleNode(this.rule);
    }

    public void updateCount(int count) {
        countLabel.setText("(" + count + ")");
        if (count > 0) {
            countLabel.setBackground(new Color(220, 255, 220)); // Light green
        } else {
            countLabel.setBackground(new Color(255, 220, 220)); // Light red
        }
    }
}
