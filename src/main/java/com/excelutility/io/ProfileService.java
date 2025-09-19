package com.excelutility.io;

import com.excelutility.core.ComparisonProfile;
import com.excelutility.core.Profile;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for saving, loading, and managing profiles.
 */
public class ProfileService {

    private final ObjectMapper mapper;
    private final String profileDirectory;
    private final File indexFile;
    public static final String COMPARISON_PROFILES_DIR = "profiles/comparison";
    public static final String FILTER_PROFILES_DIR = "profiles/filter";

    public ProfileService(String profileDirectory) {
        this.profileDirectory = profileDirectory;
        this.mapper = new ObjectMapper();
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        this.indexFile = new File(profileDirectory, "profiles.index.json");

        // Ensure directory exists
        new File(profileDirectory).mkdirs();
    }

    public <T> void saveProfile(T profile, String profileId, String profileName) throws IOException {
        File tempFile = File.createTempFile("profile-", ".json", new File(profileDirectory));
        mapper.writeValue(tempFile, profile);
        File profileFile = new File(profileDirectory, profileId + ".json");
        Files.move(tempFile.toPath(), profileFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        updateProfileIndex(profileId, profileName);
    }

    public <T> T loadProfile(String profileId, Class<T> profileClass) throws IOException {
        File profileFile = new File(profileDirectory, profileId + ".json");
        return mapper.readValue(profileFile, profileClass);
    }

    public List<String> getAvailableProfiles() {
        List<String> profileNames = new ArrayList<>();
        Map<String, String> index = loadProfileIndex();
        profileNames.addAll(index.keySet());
        return profileNames;
    }

    public void deleteProfile(String profileName) throws IOException {
        Map<String, String> index = loadProfileIndex();
        String profileId = index.get(profileName);
        if (profileId != null) {
            File profileFile = new File(profileDirectory, profileId + ".json");
            if (profileFile.exists()) {
                profileFile.delete();
            }
            removeProfileFromIndex(profileName);
        }
    }

    public Map<String, String> loadProfileIndex() {
        if (!indexFile.exists()) {
            return new HashMap<>();
        }
        try {
            return mapper.readValue(indexFile, new TypeReference<Map<String, String>>() {});
        } catch (IOException e) {
            return new HashMap<>();
        }
    }

    private void updateProfileIndex(String profileId, String profileName) throws IOException {
        Map<String, String> index = loadProfileIndex();
        index.put(profileName, profileId);
        mapper.writeValue(indexFile, index);
    }

    private void removeProfileFromIndex(String profileName) throws IOException {
        Map<String, String> index = loadProfileIndex();
        index.remove(profileName);
        mapper.writeValue(indexFile, index);
    }
}
