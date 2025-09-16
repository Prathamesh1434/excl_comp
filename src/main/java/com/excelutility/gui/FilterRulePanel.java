package com.excelutility.gui;

import com.excelutility.core.FilterRule;
import com.excelutility.core.expression.FilterExpression;
import com.excelutility.core.expression.RuleNode;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * A panel that displays a single filter rule and provides actions for it.
 */
public class FilterRulePanel extends JPanel implements ExpressionNodeComponent {

    private final FilterRule rule;

    /**
     * Constructs a panel for a given filter rule.
     *
     * @param rule           The {@link FilterRule} to display.
     * @param panelProvider  A reference to the main FilterPanel to call back to for actions.
     * @param deleteListener The {@link ActionListener} to be invoked when the delete button is clicked.
     */
    public FilterRulePanel(FilterRule rule, FilterPanel panelProvider, ActionListener deleteListener) {
        this.rule = rule;
        setLayout(new MigLayout("insets 5, fillx", "[grow][][][]"));
        setBorder(BorderFactory.createEtchedBorder());

        JLabel ruleLabel = new JLabel(rule.getDescriptiveName());
        add(ruleLabel, "growx");

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
}
