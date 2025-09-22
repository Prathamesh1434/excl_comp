package com.excelutility.gui;

import com.excelutility.core.FilteringService;
import com.excelutility.core.expression.FilterExpression;
import com.excelutility.core.expression.GroupNode;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A panel that represents a logical grouping of other filter components.
 * This version supports infix operators (AND/OR between each component).
 */
public class LogicalGroupPanel extends JPanel implements ExpressionNodeComponent {

    private final JTextField groupNameField;
    private final JPanel contentPanel;
    private final JButton addRuleButton;
    private final JLabel recordCountLabel;
    private final OperatorPanel groupOperatorPanel;

    /**
     * A panel for the AND/OR radio buttons.
     */
    private static class OperatorPanel extends JPanel {
        private final JRadioButton andButton;
        private final JRadioButton orButton;

        public OperatorPanel() {
            super(new FlowLayout(FlowLayout.CENTER, 5, 0));
            andButton = new JRadioButton("AND");
            orButton = new JRadioButton("OR");

            ButtonGroup group = new ButtonGroup();
            group.add(andButton);
            group.add(orButton);
            andButton.setSelected(true); // Default to AND

            add(andButton);
            add(orButton);
        }

        public FilteringService.LogicalOperator getOperator() {
            return andButton.isSelected() ? FilteringService.LogicalOperator.AND : FilteringService.LogicalOperator.OR;
        }

        public void setOperator(FilteringService.LogicalOperator op) {
            if (op == FilteringService.LogicalOperator.AND) {
                andButton.setSelected(true);
            } else {
                orButton.setSelected(true);
            }
        }
    }

    public LogicalGroupPanel(String initialName, ActionListener deleteListener) {
        super(new MigLayout("insets 0, fillx, wrap 1", "[grow]"));
        setBorder(BorderFactory.createTitledBorder(initialName));
        setBackground(Color.WHITE);

        JPanel topBar = new JPanel(new MigLayout("insets 2 5 2 5, fillx", "[grow]push[]"));
        topBar.setBackground(new Color(220, 235, 255));

        groupNameField = new JTextField(initialName);
        groupNameField.setBorder(null);
        groupNameField.setBackground(topBar.getBackground());
        topBar.add(groupNameField, "growx, wmin 100");

        groupOperatorPanel = new OperatorPanel();
        groupOperatorPanel.setBackground(topBar.getBackground());
        groupOperatorPanel.andButton.setBackground(topBar.getBackground());
        groupOperatorPanel.orButton.setBackground(topBar.getBackground());
        topBar.add(groupOperatorPanel, "gapleft 10");

        recordCountLabel = new JLabel("(N/A)");
        recordCountLabel.setFont(recordCountLabel.getFont().deriveFont(Font.BOLD));
        topBar.add(recordCountLabel, "gapleft 10");

        addRuleButton = new JButton("Add Rule");
        topBar.add(addRuleButton);

        if (deleteListener != null) {
            JButton deleteGroupButton = new JButton("X");
            deleteGroupButton.setToolTipText("Delete this group");
            deleteGroupButton.setMargin(new Insets(1, 1, 1, 1));
            deleteGroupButton.addActionListener(e -> deleteListener.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null)));
            topBar.add(deleteGroupButton);
        }

        add(topBar, "growx");

        contentPanel = new JPanel(new MigLayout("insets 5 10 5 10, fillx, wrap 1", "[grow]"));
        add(contentPanel, "growx");
    }

    public FilteringService.LogicalOperator getOperator() {
        return groupOperatorPanel.getOperator();
    }

    public void setOperator(FilteringService.LogicalOperator operator) {
        groupOperatorPanel.setOperator(operator);
    }

    @Override
    public String getName() {
        return groupNameField.getText();
    }

    @Override
    public void setName(String name) {
        groupNameField.setText(name);
        setBorder(BorderFactory.createTitledBorder(name));
    }


    @Override
    public void setBorder(javax.swing.border.Border border) {
        if (groupNameField != null && border instanceof javax.swing.border.TitledBorder) {
            groupNameField.setText(((javax.swing.border.TitledBorder) border).getTitle());
        }
        super.setBorder(border);
    }

    public void addComponent(Component component) {
        if (!(component instanceof ExpressionNodeComponent)) {
            throw new IllegalArgumentException("Only ExpressionNodeComponents can be added to a LogicalGroupPanel.");
        }
        contentPanel.add(component, "growx, gaptop 5");
        revalidateAndRepaint();
    }

    public void removeComponent(Component component) {
        contentPanel.remove(component);
        revalidateAndRepaint();
    }

    private void revalidateAndRepaint() {
        contentPanel.revalidate();
        contentPanel.repaint();
        if (getParent() != null) {
            getParent().revalidate();
            getParent().repaint();
        }
    }

    @Override
    public void removeAll() {
        contentPanel.removeAll();
        revalidateAndRepaint();
    }

    @Override
    public FilterExpression getExpression() {
        GroupNode groupNode = new GroupNode(getOperator(), getName());
        Arrays.stream(contentPanel.getComponents())
                .filter(c -> c instanceof ExpressionNodeComponent)
                .map(c -> ((ExpressionNodeComponent) c).getExpression())
                .forEach(groupNode::addChild);
        return groupNode;
    }

    public JButton getAddRuleButton() {
        return addRuleButton;
    }

    public JPanel getContentPanel() {
        return contentPanel;
    }

    public void setRecordCount(int count) {
        if (count < 0) {
            recordCountLabel.setText("(Error)");
            recordCountLabel.setForeground(Color.ORANGE);
        } else {
            recordCountLabel.setText("(" + count + ")");
            if (count == 0) {
                recordCountLabel.setForeground(Color.RED);
            } else {
                recordCountLabel.setForeground(new Color(0, 153, 0)); // Dark Green
            }
        }
    }
}
