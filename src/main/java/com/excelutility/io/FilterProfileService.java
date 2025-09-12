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
 * Service for saving and loading filter profiles to/from YAML files.
 */
public class FilterProfileService {

    private final Path profileDir;
    private final ObjectMapper mapper;

    public FilterProfileService() {
        this("profiles/filter");
    }

    public FilterProfileService(String profileDirectory) {
        this.profileDir = Paths.get(profileDirectory);
        try {
            Files.createDirectories(profileDir);
        } catch (IOException e) {
            throw new RuntimeException("Could not create profile directory: " + profileDir, e);
        }
        this.mapper = new ObjectMapper(new YAMLFactory().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER));
    }

    public void saveProfile(FilterProfile profile, String profileName) throws IOException {
        String fileName = profileName.endsWith(".yml") ? profileName : profileName + ".yml";
        File profileFile = profileDir.resolve(fileName).toFile();
        mapper.writeValue(profileFile, profile);
    }

    public FilterProfile loadProfile(String profileName) throws IOException {
        String fileName = profileName.endsWith(".yml") ? profileName : profileName + ".yml";
        File profileFile = profileDir.resolve(fileName).toFile();
        return mapper.readValue(profileFile, FilterProfile.class);
    }

    public List<String> getAvailableProfiles() {
        try (Stream<Path> stream = Files.list(profileDir)) {
            return stream
                    .filter(file -> !Files.isDirectory(file))
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.toLowerCase().endsWith(".yml"))
                    .map(name -> name.substring(0, name.length() - 4))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }

    public void deleteProfile(String profileName) throws IOException {
        String fileName = profileName.endsWith(".yml") ? profileName : profileName + ".yml";
        Files.deleteIfExists(profileDir.resolve(fileName));
    }
}
