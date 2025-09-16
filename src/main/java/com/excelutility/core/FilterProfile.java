package com.excelutility.core;

import com.excelutility.core.expression.FilterExpression;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class FilterProfile {

    private static final int CURRENT_VERSION = 1;

    private final int version;
    private final String profileName;
    private final FilterExpression rootExpression;
    private final String dataFilePath;
    private final String filterValuesFilePath;
    private final String dataFileSheet;
    private final String filterValuesFileSheet;

    @JsonCreator
    public FilterProfile(
            @JsonProperty("version") int version,
            @JsonProperty("profileName") String profileName,
            @JsonProperty("rootExpression") FilterExpression rootExpression,
            @JsonProperty("dataFilePath") String dataFilePath,
            @JsonProperty("filterValuesFilePath") String filterValuesFilePath,
            @JsonProperty("dataFileSheet") String dataFileSheet,
            @JsonProperty("filterValuesFileSheet") String filterValuesFileSheet) {
        this.version = version;
        this.profileName = profileName;
        this.rootExpression = rootExpression;
        this.dataFilePath = dataFilePath;
        this.filterValuesFilePath = filterValuesFilePath;
        this.dataFileSheet = dataFileSheet;
        this.filterValuesFileSheet = filterValuesFileSheet;
    }

    public FilterProfile(String profileName, FilterExpression rootExpression, String dataFilePath, String filterValuesFilePath, String dataFileSheet, String filterValuesFileSheet) {
        this(CURRENT_VERSION, profileName, rootExpression, dataFilePath, filterValuesFilePath, dataFileSheet, filterValuesFileSheet);
    }

    // Getters for all fields
    public int getVersion() { return version; }
    public String getProfileName() { return profileName; }
    public FilterExpression getRootExpression() { return rootExpression; }
    public String getDataFilePath() { return dataFilePath; }
    public String getFilterValuesFilePath() { return filterValuesFilePath; }
    public String getDataFileSheet() { return dataFileSheet; }
    public String getFilterValuesFileSheet() { return filterValuesFileSheet; }
}
