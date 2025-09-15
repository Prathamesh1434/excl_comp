package com.excelutility.io;

import com.excelutility.core.FilterProfile;
import com.excelutility.core.FilterRule;
import com.excelutility.core.FilteringService;
import com.excelutility.core.expression.GroupNode;
import com.excelutility.core.expression.RuleNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class FilterProfileServiceTest {

    private final String TEST_PROFILE_DIR = "target/test-profiles";
    private FilterProfileService profileService;
    private Path profileDirPath;

    @BeforeEach
    void setUp() throws IOException {
        profileDirPath = Paths.get(TEST_PROFILE_DIR);
        Files.createDirectories(profileDirPath);
        profileService = new FilterProfileService(TEST_PROFILE_DIR);
    }

    @AfterEach
    void tearDown() throws IOException {
        // Clean up the test directory and its contents
        Files.walk(profileDirPath)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(java.io.File::delete);
    }

    @Test
    void testSaveAndLoadProfile() throws IOException {
        // 1. Create a sample profile object
        GroupNode root = new GroupNode(FilteringService.LogicalOperator.AND, "Root");
        root.addChild(new RuleNode(new FilterRule(FilterRule.SourceType.BY_VALUE, "Active", "Status", true)));
        List<String> columns = Arrays.asList("ID", "Name", "Status");
        String sheetName = "Sheet1";
        String profileName = "Test Profile 1";

        FilterProfile originalProfile = new FilterProfile(profileName, root, sheetName, columns);

        // 2. Save the profile
        profileService.saveProfile(originalProfile);

        // 3. Verify the file exists
        assertTrue(Files.exists(profileDirPath.resolve(profileName + ".json")));

        // 4. Load the profile back
        FilterProfile loadedProfile = profileService.loadProfile(profileName);

        // 5. Assert that the loaded data matches the original
        assertNotNull(loadedProfile);
        assertEquals(originalProfile.getProfileName(), loadedProfile.getProfileName());
        assertEquals(originalProfile.getSelectedSheet(), loadedProfile.getSelectedSheet());
        assertEquals(originalProfile.getDisplayColumns(), loadedProfile.getDisplayColumns());
        assertNotNull(loadedProfile.getCreatedAt()); // Should be populated on creation

        // 6. Assert that the expression tree is equivalent
        assertNotNull(loadedProfile.getRootExpression());
        assertTrue(loadedProfile.getRootExpression() instanceof GroupNode);
        GroupNode loadedRoot = (GroupNode) loadedProfile.getRootExpression();
        assertEquals(FilteringService.LogicalOperator.AND, loadedRoot.getOperator());
        assertEquals(1, loadedRoot.getChildren().size());
        assertTrue(loadedRoot.getChildren().get(0) instanceof RuleNode);
        RuleNode loadedRule = (RuleNode) loadedRoot.getChildren().get(0);
        assertEquals("Status", loadedRule.getRule().getTargetColumn());
        assertEquals("Active", loadedRule.getRule().getSourceValue());
    }

    @Test
    void testProfileVersioning() throws IOException {
        // 1. Create two profiles with the same name
        String profileName = "Versioned Profile";
        FilterProfile profile1 = new FilterProfile(profileName, new GroupNode(FilteringService.LogicalOperator.AND, "Root"), "Sheet1", List.of("A"));
        FilterProfile profile2 = new FilterProfile(profileName, new GroupNode(FilteringService.LogicalOperator.OR, "Root"), "Sheet2", List.of("B"));

        // 2. Save both
        profileService.saveProfile(profile1);
        profileService.saveProfile(profile2);

        // 3. Verify that two files were created with versioning
        assertTrue(Files.exists(profileDirPath.resolve("Versioned Profile.json")));
        assertTrue(Files.exists(profileDirPath.resolve("Versioned Profile-v2.json")));

        // 4. Verify that getAvailableProfiles returns both
        List<String> available = profileService.getAvailableProfiles();
        assertEquals(2, available.size());
        assertTrue(available.contains("Versioned Profile"));
        assertTrue(available.contains("Versioned Profile-v2"));

        // 5. Load both and check their contents to ensure they are distinct
        FilterProfile loaded1 = profileService.loadProfile("Versioned Profile");
        FilterProfile loaded2 = profileService.loadProfile("Versioned Profile-v2");

        assertEquals("Sheet1", loaded1.getSelectedSheet());
        assertEquals("Sheet2", loaded2.getSelectedSheet());
        assertEquals(FilteringService.LogicalOperator.AND, ((GroupNode) loaded1.getRootExpression()).getOperator());
        assertEquals(FilteringService.LogicalOperator.OR, ((GroupNode) loaded2.getRootExpression()).getOperator());
    }
}
