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
import javax.swing.JOptionPane;

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
        // The profile name is now part of the profile object itself
        String profileName = profile.getProfileName();
        String fileName = profileName.endsWith(".json") ? profileName : profileName + ".json";
        File profileFile = profileDir.resolve(fileName).toFile();

        // Check for overwrite
        if (profileFile.exists()) {
            int result = JOptionPane.showConfirmDialog(null,
                    "A profile with the name '" + profileName + "' already exists. Do you want to overwrite it?",
                    "Confirm Overwrite",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (result == JOptionPane.NO_OPTION) {
                // User chose not to overwrite, so we can just return.
                // We throw an exception that the calling code can catch to prevent showing a success message.
                throw new IOException("Save cancelled by user.");
            }
        }
        mapper.writeValue(profileFile, profile);
    }

    public FilterProfile loadProfile(String profileName) throws IOException {
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
                    .map(name -> name.substring(0, name.length() - 5))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }

    public void deleteProfile(String profileName) throws IOException {
        String fileName = profileName.endsWith(".json") ? profileName : profileName + ".json";
        Files.deleteIfExists(profileDir.resolve(fileName));
    }
}
