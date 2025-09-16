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
import java.util.stream.Collectors;

/**
 * A panel that represents a logical grouping of other filter components
 * (either individual rules or other logical groups).
 */
public class LogicalGroupPanel extends JPanel implements ExpressionNodeComponent {

    private final JPanel contentPanel;
    private final JButton addRuleButton;
    private final JTextField groupNameField;
    private final JLabel countLabel;
    private final FilterPanel filterPanel;
    private final List<Component> childFilterComponents = new ArrayList<>();

    /**
     * Constructs a new LogicalGroupPanel.
     *
     * @param initialName        The initial name to display for the group.
     * @param deleteListener     The listener to be called when this entire group is deleted. Can be null for the root panel.
     * @param showAddGroupButton If true, the "Add Group" button will be visible for this panel.
     * @param filterPanel        A reference to the main FilterPanel for callbacks.
     */
    public LogicalGroupPanel(String initialName, ActionListener deleteListener, boolean showAddGroupButton, FilterPanel filterPanel) {
        this.filterPanel = filterPanel;
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 1, 1, 1, Color.GRAY),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        setLayout(new MigLayout("insets 0, fillx, wrap 1", "[grow]"));

        // --- Top Bar ---
        JPanel topBar = new JPanel(new MigLayout("insets 0", "[][grow]push[]"));
        groupNameField = new JTextField(initialName);
        topBar.add(new JLabel("Group Name:"));
        topBar.add(groupNameField, "growx");

        countLabel = new JLabel("(?)");
        countLabel.setOpaque(true);
        topBar.add(countLabel, "gapleft 10");

        addRuleButton = new JButton("Add Rule");
        JButton addGroupButton = new JButton("Add Group");
        addGroupButton.setVisible(showAddGroupButton);
        JButton viewButton = new JButton("View");
        viewButton.addActionListener(e -> {
            if (this.filterPanel != null) {
                this.filterPanel.viewResultsForGroup(this);
            }
        });
        JButton deleteGroupButton = new JButton("Delete Group");

        if (deleteListener != null) {
            deleteGroupButton.addActionListener(e -> deleteListener.actionPerformed(
                    new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null))
            );
        } else {
            deleteGroupButton.setVisible(false);
        }

        topBar.add(addRuleButton, "split 4, gapleft 20");
        topBar.add(addGroupButton);
        topBar.add(viewButton);
        topBar.add(deleteGroupButton);

        add(topBar, "growx");

        // --- Content Panel for Rules/Groups ---
        contentPanel = new JPanel(new MigLayout("insets 5 0 0 0, fillx, wrap 1", "[grow]"));
        contentPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY));
        add(contentPanel, "growx, gaptop 5");

        // --- Action Listeners ---
        addGroupButton.addActionListener(e -> {
            String newGroupName = com.excelutility.core.AutoNamingService.suggestGroupName();
            ActionListener newDeleteListener = event -> {
                LogicalGroupPanel sourceGroup = (LogicalGroupPanel) event.getSource();
                removeComponent(sourceGroup);
            };
            LogicalGroupPanel newGroup = new LogicalGroupPanel(newGroupName, newDeleteListener, true, this.filterPanel);
            filterPanel.configureGroupPanel(newGroup);
            addComponent(newGroup);
        });
    }

    public JButton getAddRuleButton() {
        return addRuleButton;
    }

    public String getGroupName() {
        return groupNameField.getText();
    }

    public void setGroupName(String name) {
        groupNameField.setText(name);
    }

    public void setOperator(FilteringService.LogicalOperator operator) {
        // This method is now a no-op as operators are handled by ConnectorPanels.
        // It's kept for API compatibility during profile loading, but could be removed later.
    }

    @Override
    public void removeAll() {
        contentPanel.removeAll();
        childFilterComponents.clear();
        revalidateAndRepaint();
    }

    public void addComponent(Component component) {
        if (!(component instanceof ExpressionNodeComponent)) {
            return; // Only add components that can be part of an expression
        }
        // If there's already at least one component, add a connector first.
        if (!childFilterComponents.isEmpty()) {
            contentPanel.add(new ConnectorPanel(), "growx");
        }
        contentPanel.add(component, "growx");
        childFilterComponents.add(component);
        revalidateAndRepaint();
    }

    /**
     * Adds a pre-configured connector during UI reconstruction from a profile.
     * @param connector The connector to add.
     */
    public void rebuildConnector(ConnectorPanel connector) {
        contentPanel.add(connector, "growx");
        revalidateAndRepaint();
    }

    /**
     * Adds a component during UI reconstruction without adding a connector.
     * @param component The component (rule or group) to add.
     */
    public void rebuildComponent(Component component) {
        if (!(component instanceof ExpressionNodeComponent)) {
            return;
        }
        contentPanel.add(component, "growx");
        childFilterComponents.add(component);
        revalidateAndRepaint();
    }

    public void removeComponent(Component component) {
        int index = childFilterComponents.indexOf(component);
        if (index == -1) {
            return;
        }

        childFilterComponents.remove(component);

        // The actual components in contentPanel are (Component, Connector, Component, ...)
        // A component at index `i` in childFilterComponents is at `i*2` in contentPanel.
        int contentIndex = index * 2;
        contentPanel.remove(contentIndex);

        // If we removed a component that wasn't the first one, we also remove the connector before it.
        if (contentIndex > 0) {
            contentPanel.remove(contentIndex - 1);
        }
        // If we removed the first component and there are others left, the new first component doesn't need a preceding connector.
        else if (!childFilterComponents.isEmpty()) {
            // The connector that was at index 1 is now at 0. Remove it.
            contentPanel.remove(0);
        }

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
    public FilterExpression getExpression() {
        List<Component> allComponents = List.of(contentPanel.getComponents());

        List<ExpressionNodeComponent> nodes = allComponents.stream()
                .filter(c -> c instanceof ExpressionNodeComponent)
                .map(c -> (ExpressionNodeComponent) c)
                .collect(Collectors.toList());

        List<ConnectorPanel> connectors = allComponents.stream()
                .filter(c -> c instanceof ConnectorPanel)
                .map(c -> (ConnectorPanel) c)
                .collect(Collectors.toList());

        if (nodes.isEmpty()) {
            return new GroupNode(FilteringService.LogicalOperator.AND, getGroupName()); // Return empty group
        }

        if (nodes.size() == 1) {
            // If there's only one node, wrap it in this panel's group
            GroupNode group = new GroupNode(FilteringService.LogicalOperator.AND, getGroupName());
            group.addChild(nodes.get(0).getExpression());
            return group;
        }

        // Build the expression tree from left to right based on the connectors
        FilterExpression currentExpression = nodes.get(0).getExpression();
        for (int i = 0; i < connectors.size(); i++) {
            FilteringService.LogicalOperator op = connectors.get(i).getOperator();
            FilterExpression nextNodeExpression = nodes.get(i + 1).getExpression();

            // Create a new group to combine the current expression with the next one
            GroupNode newGroup = new GroupNode(op, ""); // Intermediate groups are unnamed
            newGroup.addChild(currentExpression);
            newGroup.addChild(nextNodeExpression);
            currentExpression = newGroup;
        }

        // Finally, wrap the entire constructed expression in a group with this panel's name
        GroupNode finalGroup = new GroupNode(FilteringService.LogicalOperator.AND, getGroupName());
        finalGroup.addChild(currentExpression);
        return finalGroup;
    }

    public void updateCount(int count) {
        countLabel.setText("(" + count + ")");
        if (count > 0) {
            countLabel.setBackground(new Color(220, 255, 220)); // Light green
        } else if (count == 0) {
            countLabel.setBackground(new Color(255, 220, 220)); // Light red
        } else { // count < 0 indicates an error
            countLabel.setBackground(Color.ORANGE);
            countLabel.setText("(Error)");
        }
    }
}
