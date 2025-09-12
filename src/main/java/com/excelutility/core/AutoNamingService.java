package com.excelutility.core;

/**
 * A service for automatically generating names for filter groups.
 */
public class AutoNamingService {

    private static int groupCounter = 1;

    /**
     * Suggests a default name for a new filter group.
     * @return A string like "Group 1", "Group 2", etc.
     */
    public static String suggestGroupName() {
        return "Group " + (groupCounter++);
    }

    /**
     * Resets the counter for group names. Useful for when clearing all filters.
     */
    public static void reset() {
        groupCounter = 1;
    }
}
