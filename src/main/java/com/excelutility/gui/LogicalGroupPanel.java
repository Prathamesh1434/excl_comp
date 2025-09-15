package com.excelutility.gui;

import com.excelutility.core.FilteringService;
import com.excelutility.core.expression.FilterExpression;
import com.excelutility.core.expression.GroupNode;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

/**
 * A panel that represents a logical grouping (AND/OR) of other filter components
 * (either individual rules or other logical groups).
 */
public class LogicalGroupPanel extends JPanel implements ExpressionNodeComponent {

    private final JPanel contentPanel;
    private final JRadioButton andButton;
    private final JRadioButton orButton;
    private final JButton addRuleButton;
    private final JButton addGroupButton;
    private final JTextField groupNameField;
    private final List<Component> childComponents = new ArrayList<>();

    /**
     * Constructs a new LogicalGroupPanel.
     *
     * @param initialName    The initial name to display for the group.
     * @param deleteListener The listener to be called when this entire group is deleted. Can be null for the root panel.
     */
    public LogicalGroupPanel(String initialName, ActionListener deleteListener) {
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 1, 1, 1, Color.GRAY),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        setLayout(new MigLayout("insets 0, fillx, wrap 1", "[grow]"));

        JPanel topBar = new JPanel(new MigLayout("insets 0", "[][grow]push[]"));
        groupNameField = new JTextField(initialName);
        topBar.add(groupNameField, "growx");

        // --- Radio Buttons for Logic ---
        andButton = new JRadioButton("AND");
        andButton.setSelected(true); // Default selection
        orButton = new JRadioButton("OR");

        ButtonGroup logicGroup = new ButtonGroup();
        logicGroup.add(andButton);
        logicGroup.add(orButton);

        JPanel logicPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        logicPanel.add(new JLabel("Group Logic:"));
        logicPanel.add(andButton);
        logicPanel.add(orButton);
        topBar.add(logicPanel);

        addRuleButton = new JButton("Add Rule");
        addGroupButton = new JButton("Add Group");
        JButton deleteGroupButton = new JButton("Delete Group");

        if (deleteListener != null) {
            deleteGroupButton.addActionListener(e -> deleteListener.actionPerformed(
                    new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null))
            );
        } else {
            deleteGroupButton.setVisible(false);
        }

        topBar.add(addRuleButton, "split 3, gapleft 20");
        topBar.add(addGroupButton);
        topBar.add(deleteGroupButton);

        add(topBar, "growx");

        contentPanel = new JPanel(new MigLayout("insets 5 0 0 0, fillx, wrap 1", "[grow]"));
        contentPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY));
        add(contentPanel, "growx, gaptop 5");
    }

    public JButton getAddRuleButton() {
        return addRuleButton;
    }

    public JButton getAddGroupButton() {
        return addGroupButton;
    }

    public void setGroupName(String name) {
        groupNameField.setText(name);
    }

    public void setOperator(FilteringService.LogicalOperator operator) {
        if (operator == FilteringService.LogicalOperator.AND) {
            andButton.setSelected(true);
        } else {
            orButton.setSelected(true);
        }
    }

    @Override
    public void removeAll() {
        childComponents.clear();
        contentPanel.removeAll();
        revalidateAndRepaint();
    }

    public void addComponent(Component component) {
        childComponents.add(component);
        contentPanel.add(component, "growx");
        revalidateAndRepaint();
    }

    public void removeComponent(Component component) {
        childComponents.remove(component);
        contentPanel.remove(component);
        revalidateAndRepaint();
    }

    private void revalidateAndRepaint() {
        contentPanel.revalidate();
        contentPanel.repaint();
        // Also revalidate the parent container
        if (getParent() != null) {
            getParent().revalidate();
            getParent().repaint();
        }
    }

    @Override
    public FilterExpression getExpression() {
        String name = groupNameField.getText();
        FilteringService.LogicalOperator op = andButton.isSelected() ? FilteringService.LogicalOperator.AND : FilteringService.LogicalOperator.OR;
        GroupNode groupNode = new GroupNode(op, name);

        for (Component child : childComponents) {
            if (child instanceof ExpressionNodeComponent) {
                groupNode.addChild(((ExpressionNodeComponent) child).getExpression());
            }
        }
        return groupNode;
    }
}
