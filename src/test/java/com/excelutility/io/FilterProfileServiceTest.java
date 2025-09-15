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
}
