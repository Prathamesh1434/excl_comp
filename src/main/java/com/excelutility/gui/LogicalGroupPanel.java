package com.excelutility.gui;

import com.excelutility.core.AutoNamingService;
import com.excelutility.core.FilteringService;
import com.excelutility.core.expression.FilterExpression;
import com.excelutility.core.expression.GroupNode;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class LogicalGroupPanel extends JPanel implements ExpressionNodeComponent {

    private final JPanel contentPanel;
    private final JButton addRuleButton;
    private final JButton addGroupButton;
    private final JTextField groupNameField;
    private final JCheckBox selectionCheckBox;
    private final FilterPanel filterPanel;

    public LogicalGroupPanel(String initialName, ActionListener deleteListener, FilterPanel filterPanel) {
        this.filterPanel = filterPanel;
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 1, 1, 1, Color.GRAY),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        setLayout(new MigLayout("insets 0, fillx, wrap 1", "[grow]"));

        JPanel topBar = new JPanel(new MigLayout("insets 0, hidemode 3", "[]5[grow]push[]"));

        selectionCheckBox = new JCheckBox();
        topBar.add(selectionCheckBox);

        groupNameField = new JTextField(initialName);
        groupNameField.setEditable(false);
        groupNameField.setBorder(BorderFactory.createEmptyBorder());
        topBar.add(new JLabel("Group:"), "split 2, shrinkx");
        topBar.add(groupNameField, "growx");

        addRuleButton = new JButton("Add Rule");
        addGroupButton = new JButton("Add Group");
        JButton wrapButton = new JButton("Wrap Selection");
        JButton renameButton = new JButton("Rename");
        JButton deleteGroupButton = new JButton("Delete");

        renameButton.addActionListener(e -> {
            boolean isEditable = !groupNameField.isEditable();
            groupNameField.setEditable(isEditable);
            groupNameField.setBorder(isEditable ? BorderFactory.createLineBorder(Color.GRAY) : BorderFactory.createEmptyBorder());
        });

        if (deleteListener != null) {
            deleteGroupButton.addActionListener(e -> deleteListener.actionPerformed(
                    new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null))
            );
        } else {
            deleteGroupButton.setVisible(false);
            selectionCheckBox.setVisible(false);
        }

        wrapButton.addActionListener(e -> wrapSelection());

        topBar.add(addRuleButton, "split 5, gapleft 20");
        topBar.add(addGroupButton);
        topBar.add(wrapButton);
        topBar.add(renameButton);
        topBar.add(deleteGroupButton);

        add(topBar, "growx");

        contentPanel = new JPanel(new MigLayout("insets 5 0 0 0, fillx, wrap 1", "[grow]"));
        contentPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY));
        add(contentPanel, "growx, gaptop 5");
    }

    private void wrapSelection() {
        List<ExpressionNodeComponent> selectedNodes = Arrays.stream(contentPanel.getComponents())
                .filter(c -> c instanceof ExpressionNodeComponent && ((ExpressionNodeComponent) c).isSelected())
                .map(c -> (ExpressionNodeComponent) c)
                .collect(Collectors.toList());

        if (selectedNodes.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select one or more items to wrap.", "Selection Required", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Find the index of the first selected component to insert the new group at
        Component firstSelectedComp = selectedNodes.get(0).getComponent();
        int insertionIndex = -1;
        List<Component> allComponents = Arrays.asList(contentPanel.getComponents());
        for (int i = 0; i < allComponents.size(); i++) {
            if (allComponents.get(i) == firstSelectedComp) {
                insertionIndex = i;
                break;
            }
        }

        // Remove the selected components from the current panel
        for (ExpressionNodeComponent node : selectedNodes) {
            this.removeComponent(node.getComponent());
        }

        // Create the new group that will contain the selected items
        String newGroupName = AutoNamingService.suggestGroupName();
        LogicalGroupPanel newGroup = new LogicalGroupPanel(newGroupName, e -> this.removeComponent((Component) e.getSource()), this.filterPanel);
        this.filterPanel.configureGroupPanel(newGroup); // Configure buttons on the new group

        // Add the selected components to the new group.
        // The addComponent method will handle adding connectors.
        for (ExpressionNodeComponent node : selectedNodes) {
            newGroup.addComponent(node.getComponent());
        }

        // Add the new group to this panel at the correct position
        if (insertionIndex != -1) {
            if (insertionIndex > 0) {
                 contentPanel.add(new ConnectorPanel(), "growx", insertionIndex -1);
            }
            contentPanel.add(newGroup, "growx", insertionIndex);
        } else {
            this.addComponent(newGroup);
        }

        revalidateAndRepaint();
    }

    public JButton getAddRuleButton() { return addRuleButton; }
    public JButton getAddGroupButton() { return addGroupButton; }
    public String getGroupName() { return groupNameField.getText(); }
    public void setGroupName(String name) { groupNameField.setText(name); }

    public void addComponent(Component component) {
        if (!(component instanceof ExpressionNodeComponent)) return;
        if (contentPanel.getComponentCount() > 0) {
            contentPanel.add(new ConnectorPanel(), "growx");
        }
        contentPanel.add(component, "growx");
        revalidateAndRepaint();
    }

    public void removeComponent(Component component) {
        List<Component> components = Arrays.asList(contentPanel.getComponents());
        int index = components.indexOf(component);
        if (index == -1) return;

        contentPanel.remove(component);
        if (index > 0 && components.get(index - 1) instanceof ConnectorPanel) {
            contentPanel.remove(index - 1);
        } else if (index == 0 && contentPanel.getComponentCount() > 0 && contentPanel.getComponent(0) instanceof ConnectorPanel) {
            contentPanel.remove(0);
        }
        revalidateAndRepaint();
    }

    @Override
    public void removeAll() {
        contentPanel.removeAll();
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
        List<Component> allComponents = Arrays.asList(contentPanel.getComponents());
        List<ExpressionNodeComponent> nodes = allComponents.stream()
                .filter(c -> c instanceof ExpressionNodeComponent)
                .map(c -> (ExpressionNodeComponent) c)
                .collect(Collectors.toList());
        List<ConnectorPanel> connectors = allComponents.stream()
                .filter(c -> c instanceof ConnectorPanel)
                .map(c -> (ConnectorPanel) c)
                .collect(Collectors.toList());

        if (nodes.isEmpty()) {
            return new GroupNode(FilteringService.LogicalOperator.AND, getGroupName());
        }

        FilterExpression finalExpression;
        if (nodes.size() == 1) {
            finalExpression = nodes.get(0).getExpression();
        } else {
            FilterExpression currentExpression = nodes.get(0).getExpression();
            for (int i = 0; i < connectors.size(); i++) {
                FilteringService.LogicalOperator op = connectors.get(i).getOperator();
                ExpressionNodeComponent nextNode = nodes.get(i + 1);
                GroupNode newGroup = new GroupNode(op, "");
                newGroup.addChild(currentExpression);
                newGroup.addChild(nextNode.getExpression());
                currentExpression = newGroup;
            }
            finalExpression = currentExpression;
        }

        GroupNode wrapperGroup = new GroupNode(FilteringService.LogicalOperator.AND, getGroupName());
        wrapperGroup.addChild(finalExpression);
        return wrapperGroup;
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
