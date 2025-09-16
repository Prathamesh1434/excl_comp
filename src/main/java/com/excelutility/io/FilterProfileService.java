package com.excelutility.io;

import com.excelutility.core.FilterProfile;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FilterProfileService {

    private final Path profileDir;
    private final ObjectMapper mapper;

    public FilterProfileService() {
        this("profiles/spec_qa_recon");
    }

    public FilterProfileService(String profileDirectory) {
        this.profileDir = Paths.get(profileDirectory);
        try {
            Files.createDirectories(profileDir);
        } catch (IOException e) {
            throw new RuntimeException("Could not create profile directory: " + profileDir, e);
        }
        this.mapper = new ObjectMapper();
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT); // For pretty printing JSON
    }

    public void saveProfile(FilterProfile profile) throws IOException {
        String fileName = profile.getProfileName() + ".json";
        File profileFile = profileDir.resolve(fileName).toFile();
        mapper.writeValue(profileFile, profile);
    }

    public FilterProfile loadProfile(String profileName) throws IOException {
        String fileName = profileName + ".json";
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
                    .sorted()
                    .collect(Collectors.toList());
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }

    public void deleteProfile(String profileName) throws IOException {
        String fileName = profileName + ".json";
        Files.deleteIfExists(profileDir.resolve(fileName));
    }

    public void renameProfile(String oldName, String newName) throws IOException {
        String oldFileName = oldName + ".json";
        String newFileName = newName + ".json";
        Path source = profileDir.resolve(oldFileName);
        Files.move(source, source.resolveSibling(newFileName));
    }
}
