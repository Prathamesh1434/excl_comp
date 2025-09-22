package com.excelutility;

import com.excelutility.core.FilterProfile;
import com.excelutility.core.FilterBuilderState;
import com.excelutility.core.GroupState;
import com.excelutility.core.RuleState;
import com.excelutility.core.FilterRule;
import com.excelutility.core.FilteringService;
import com.excelutility.io.FilterProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ProfileEndToEndTest {

    private FilterProfileService profileService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        profileService = new FilterProfileService(tempDir);
    }

    @Test
    void testSaveAndLoadComplexProfile() throws IOException {
        // Step 1: Construct a complex profile object programmatically
        FilterProfile originalProfile = createComplexTestProfile("End-to-End Test Profile");

        // Step 2: Save the profile
        profileService.saveProfile(originalProfile);

        // Step 3: Verify the file was created
        List<File> profiles = profileService.getAvailableProfiles();
        assertEquals(1, profiles.size(), "A single profile file should have been created.");

        // Step 4: Load the profile back
        FilterProfile loadedProfile = profileService.loadProfile(profiles.get(0));

        // Step 5: Assert deep equality
        assertNotNull(loadedProfile);
        assertEquals(originalProfile, loadedProfile);
    }

    private FilterProfile createComplexTestProfile(String profileName) {
        // Create a nested structure of rules and groups
        RuleState rule1 = new RuleState(FilterRule.SourceType.BY_VALUE, "Active", "Status", true);
        RuleState rule2 = new RuleState(FilterRule.SourceType.BY_VALUE, "Chicago", "City", false); // Corrected this too, was COLUMN_VALUES
        GroupState innerGroup = new GroupState("Location and Status", FilteringService.LogicalOperator.AND, Arrays.asList(rule1, rule2), Collections.emptyList());

        RuleState rule3 = new RuleState(FilterRule.SourceType.BY_VALUE, "Bob", "Name", true);

        GroupState rootGroup = new GroupState("Root", FilteringService.LogicalOperator.OR, Collections.singletonList(rule3), Collections.singletonList(innerGroup));

        FilterBuilderState builderState = new FilterBuilderState(Collections.singletonList(rootGroup));

        return new FilterProfile(
            profileName,
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(new Date()),
            "/path/to/data.xlsx",
            "Sheet1",
            "/path/to/filters.xlsx",
            "FilterSheet",
            builderState,
            Collections.singletonList(0),
            com.excelutility.core.ConcatenationMode.BREADCRUMB,
            Collections.singletonList(1),
            com.excelutility.core.ConcatenationMode.LEAF_ONLY
        );
    }
}
