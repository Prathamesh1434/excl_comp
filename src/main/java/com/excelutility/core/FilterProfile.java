package com.excelutility.core;

import com.excelutility.core.expression.FilterExpression;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.time.Instant;
import java.util.List;

/**
 * A data class representing a saved state of the filter configuration,
 * including the entire expression tree and other metadata.
 */
public class FilterProfile {

    private final String profileName;
    private final String createdBy = "default-user"; // Hardcoded as per plan
    private final String createdAt;
    private final FilterExpression filterConfig;
    private final String selectedSheet;
    private final List<String> displayColumns;

    @JsonCreator
    public FilterProfile(
            @JsonProperty("profileName") String profileName,
            @JsonProperty("filterConfig") FilterExpression filterConfig,
            @JsonProperty("selectedSheet") String selectedSheet,
            @JsonProperty("displayColumns") List<String> displayColumns
    ) {
        this.profileName = profileName;
        this.filterConfig = filterConfig;
        this.selectedSheet = selectedSheet;
        this.displayColumns = displayColumns;
        this.createdAt = Instant.now().toString();
    }

    // Getters for all fields to allow Jackson to serialize them
    public String getProfileName() {
        return profileName;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    @JsonProperty("filterConfig")
    public FilterExpression getRootExpression() {
        return filterConfig;
    }

    public String getSelectedSheet() {
        return selectedSheet;
    }

    public List<String> getDisplayColumns() {
        return displayColumns;
    }
}
