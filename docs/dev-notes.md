# Developer Notes

This document contains technical notes for developers working on the Excel Utility project.

## Branch Information

- **Base Branch:** `feature/excel-filtering-from-another-excel`
- **Working Branch:** `enhancement/filter-ui-groups-dataview`

## Key Modules & Classes Changed

The recent UI/UX overhaul for the "SPEC QA Recon" mode involved significant changes to the following classes:

-   **`com.excelutility.gui.FilterPanel`**: The central UI controller. It orchestrates the creation of all components and contains the complex logic for loading profiles by "decompiling" a saved `GroupNode` expression back into a visual UI (`populateGroupFromNode`). It also handles the sequential, dialog-driven workflow for rule creation (`createFilterFromSelection`).
-   **`com.excelutility.gui.LogicalGroupPanel`**: Represents a container for rules and other groups. This was heavily refactored to use `ConnectorPanel`s between its children, instead of a single logic selector in its header. Its `getExpression()` method is now responsible for "compiling" the visual UI into a nested `GroupNode` tree that the backend can process.
-   **`com.excelutility.gui.ConnectorPanel`**: A new, simple component that visually represents the `AND/OR` logic between two filter components.
-   **`com.excelutility.gui.ResultTabPanel`**: A new component that encapsulates a results table and its associated "Download" and "Copy to Clipboard" action buttons.
-   **`com.excelutility.gui.FilterSourceDialog`**: Updated to include an `isCancelled()` method to better support the new sequential rule creation workflow.
-   **`com.excelutility.io.FilterProfileService`**: Refactored to handle profile versioning, saving new versions of a profile instead of overwriting.
-   **`com.excelutility.gui.FilterProfileManagerDialog`**: A new dialog for viewing, loading, and deleting all saved profile versions.

## Inter-Group Logic and Profile Loading

The most complex part of the implementation is the system that allows for intuitive, connector-based logic in the UI while maintaining compatibility with the backend's `GroupNode` expression tree model.

-   **UI Model**: The `LogicalGroupPanel` displays a list of rules and subgroups. Between each pair of components, it inserts a `ConnectorPanel` containing `AND/OR` radio buttons. This provides a clear, visual representation of the logic.
-   **"Compiling" to an Expression**: When a filter is run (or a profile is saved), the `LogicalGroupPanel.getExpression()` method is called. It traverses its children and the `ConnectorPanel`s between them, building a left-associative, nested tree of unnamed `GroupNode`s. For example, a UI of `RuleA OR RuleB AND RuleC` is converted into `GroupNode(AND, {GroupNode(OR, {RuleA, RuleB}), RuleC})`. The entire result is then wrapped in a final, named `GroupNode` representing the panel itself.
-   **"Decompiling" from a Profile**: When a profile is loaded, the `FilterPanel.populateGroupFromNode()` method performs the reverse operation. It uses a recursive helper, `flattenExpression`, to traverse the saved `GroupNode` tree. It uses a heuristic (unnamed `GroupNode`s with two children are assumed to be connectors) to decompose the nested tree back into a flat list of "leaf" nodes and a list of operators. It then uses this flat structure to rebuild the UI, adding rules, groups, and correctly configured `ConnectorPanel`s one by one.

## Dynamic Results Viewing

A key feature is the ability to view results for individual rules and groups without running the entire filter set.

-   **Mechanism**: The "View" buttons on `FilterRulePanel` and `LogicalGroupPanel` call back to public methods on the `FilterPanel`.
-   **Execution**: The `FilterPanel` then executes a filter for the specific expression in a `SwingWorker`.
-   **Display**: Once the worker is complete, it creates a new `JTable` with the results. This table is then passed to a new `ResultTabPanel`.
-   **ResultTabPanel**: This component contains the results `JScrollPane` as well as "Download" and "Copy to Clipboard" buttons. An instance of this panel is placed inside a new, closable tab.
-   **Closable Tabs**: The `TabComponent` class is used to create the custom tab UI with a close button.

## Header Detection

The header detection mechanism is located in **`com.excelutility.core.HeaderDetector`**. It works by analyzing the first K rows of a sheet (default K=20) and scoring each row based on a set of heuristics:

-   **Data Type:** Rows with a high percentage of string values are more likely to be headers.
-   **Font Style:** Rows where cells are bolded are given a higher score.
-   **Merged Regions:** Rows that are part of a merged region are considered strong candidates.
-   **Cell Formatting:** The presence of cell background colors or borders increases the score.

The dialog (`HeaderDetectionDialog`) then presents the rows with the highest scores to the user for confirmation.

## How to Run Tests

The project uses Maven to manage dependencies and run tests. To run the full suite of JUnit 5 tests, execute the following command from the root of the repository:

```sh
mvn test
```

This will compile the project and run all tests located in `src/test/java`. New tests for profile persistence have been added to `FilterProfileServiceTest.java`.
