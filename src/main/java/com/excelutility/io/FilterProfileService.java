package com.excelutility.io;

import com.excelutility.core.FilterProfile;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Service for saving and loading filter profiles to/from JSON files.
 */
public class FilterProfileService {

    private final Path profileDir;
    private final ObjectMapper mapper;

    public FilterProfileService() {
        this("profiles"); // As per spec
    }

    public FilterProfileService(String profileDirectory) {
        this.profileDir = Paths.get(profileDirectory);
        try {
            Files.createDirectories(profileDir);
        } catch (IOException e) {
            throw new RuntimeException("Could not create profile directory: " + profileDir, e);
        }
        // Use a standard JSON mapper
        this.mapper = new ObjectMapper();
        mapper.enable(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT);
    }

    public void saveProfile(FilterProfile profile) throws IOException {
        String baseName = profile.getProfileName();
        Path profilePath = profileDir.resolve(baseName + ".json");
        int version = 2;
        while (Files.exists(profilePath)) {
            profilePath = profileDir.resolve(baseName + "-v" + version + ".json");
            version++;
        }
        mapper.writeValue(profilePath.toFile(), profile);
    }

    public FilterProfile loadProfile(String profileName) throws IOException {
        // The profile name from the list might not have the extension
        String fileName = profileName.endsWith(".json") ? profileName : profileName + ".json";
        File profileFile = profileDir.resolve(fileName).toFile();
        return mapper.readValue(profileFile, FilterProfile.class);
    }

    public List<String> getAvailableProfiles() {
        try (Stream<Path> stream = Files.list(profileDir)) {
            return stream
                    .filter(file -> !Files.isDirectory(file))
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.toLowerCase().endsWith(".json"))
                    .map(name -> name.substring(0, name.length() - 5)) // Return full name without extension
                    .sorted() // Sort alphabetically
                    .collect(Collectors.toList());
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }

    public void deleteProfile(String profileName) throws IOException {
        // The profileName is the full name without extension (e.g., "MyProfile-v2")
        String fileName = profileName + ".json";
        Files.deleteIfExists(profileDir.resolve(fileName));
    }
}
