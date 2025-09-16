package com.excelutility.gui;

import com.excelutility.core.FilteringService;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;

public class ConnectorPanel extends JPanel {
    private final JComboBox<FilteringService.LogicalOperator> operatorComboBox;

    public ConnectorPanel() {
        setLayout(new MigLayout("insets 2 0 2 0, align center"));
        operatorComboBox = new JComboBox<>(new FilteringService.LogicalOperator[]{
                FilteringService.LogicalOperator.AND,
                FilteringService.LogicalOperator.OR
        });
        operatorComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof FilteringService.LogicalOperator) {
                    setText(((FilteringService.LogicalOperator) value).name());
                }
                setHorizontalAlignment(CENTER);
                return this;
            }
        });

        operatorComboBox.setUI(new javax.swing.plaf.basic.BasicComboBoxUI() {
            @Override
            protected JButton createArrowButton() {
                return new JButton() {
                    @Override
                    public int getWidth() {
                        return 0;
                    }
                };
            }
        });
        operatorComboBox.setPreferredSize(new Dimension(60, 25));

        add(new JSeparator(SwingConstants.HORIZONTAL), "growx, wrap, h 2, gapbottom 2, gaptop 2");
        add(operatorComboBox, "w 80!, h 25!");
        add(new JSeparator(SwingConstants.HORIZONTAL), "growx, wrap, h 2, gaptop 2, gapbottom 2");
    }

    public FilteringService.LogicalOperator getOperator() {
        return (FilteringService.LogicalOperator) operatorComboBox.getSelectedItem();
    }

    public void setOperator(FilteringService.LogicalOperator operator) {
        operatorComboBox.setSelectedItem(operator);
    }
}
