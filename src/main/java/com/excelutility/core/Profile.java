package com.excelutility.core;

import com.excelutility.core.expression.FilterExpression;
import java.util.List;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Date;

public class Profile {

    public static final int CURRENT_PROFILE_VERSION = 1;

    private String profileId;
    private String profileName;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Date createdAt;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Date updatedAt;
    private boolean saveFilePaths;
    private String dataFilePath;
    private String dataSheetName;
    private String filterFilePath;
    private String filterSheetName;
    private List<Integer> sourceHeaderRows;
    private ConcatenationMode sourceConcatenationMode;
    private FilterExpression rootExpression;
    private int profileVersion = CURRENT_PROFILE_VERSION;

    public Profile() {
        this.profileId = UUID.randomUUID().toString();
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }

    // Getters and setters for all fields

    public String getProfileId() {
        return profileId;
    }

    public void setProfileId(String profileId) {
        this.profileId = profileId;
    }

    public String getProfileName() {
        return profileName;
    }

    public void setProfileName(String profileName) {
        this.profileName = profileName;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isSaveFilePaths() {
        return saveFilePaths;
    }

    public void setSaveFilePaths(boolean saveFilePaths) {
        this.saveFilePaths = saveFilePaths;
    }

    public String getDataFilePath() {
        return dataFilePath;
    }

    public void setDataFilePath(String dataFilePath) {
        this.dataFilePath = dataFilePath;
    }

    public String getDataSheetName() {
        return dataSheetName;
    }

    public void setDataSheetName(String dataSheetName) {
        this.dataSheetName = dataSheetName;
    }

    public String getFilterFilePath() {
        return filterFilePath;
    }

    public void setFilterFilePath(String filterFilePath) {
        this.filterFilePath = filterFilePath;
    }

    public String getFilterSheetName() {
        return filterSheetName;
    }

    public void setFilterSheetName(String filterSheetName) {
        this.filterSheetName = filterSheetName;
    }

    public List<Integer> getSourceHeaderRows() {
        return sourceHeaderRows;
    }

    public void setSourceHeaderRows(List<Integer> sourceHeaderRows) {
        this.sourceHeaderRows = sourceHeaderRows;
    }

    public ConcatenationMode getSourceConcatenationMode() {
        return sourceConcatenationMode;
    }

    public void setSourceConcatenationMode(ConcatenationMode sourceConcatenationMode) {
        this.sourceConcatenationMode = sourceConcatenationMode;
    }

    public FilterExpression getRootExpression() {
        return rootExpression;
    }

    public void setRootExpression(FilterExpression rootExpression) {
        this.rootExpression = rootExpression;
    }

    public int getProfileVersion() {
        return profileVersion;
    }

    public void setProfileVersion(int profileVersion) {
        this.profileVersion = profileVersion;
    }
}
