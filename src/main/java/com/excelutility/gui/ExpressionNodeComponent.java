package com.excelutility.gui;

import com.excelutility.core.expression.FilterExpression;
import java.awt.Component;

/**
 * An interface for UI components that can be mapped to a node
 * in the backend FilterExpression tree. It also includes methods
 * to support UI features like selection.
 */
public interface ExpressionNodeComponent {

    /**
     * Builds and returns the backend representation of this UI component.
     * @return A {@link FilterExpression} object.
     */
    FilterExpression getExpression();

    /**
     * Checks if the component is currently selected in the UI.
     * @return true if selected, false otherwise.
     */
    boolean isSelected();

    /**
     * Returns the underlying UI component (the panel itself).
     * @return The component.
     */
    Component getComponent();
}
