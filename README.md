# Excel Utility

A production-ready, GUI-only Java Swing desktop application for comparing Excel files and filtering Excel data. This tool provides a comprehensive set of features for normalization, key mapping, detailed comparison, interactive filtering, and reporting, all configurable through an intuitive user interface.

## Application Structure

The application now launches into a **Mode Selection** screen. From here, you can choose your desired workflow. A "File" menu is always available, allowing you to go **Back to Mode Selection** at any time to switch workflows without restarting the application.

## Modes of Operation

### 1. Compare Excel Files (Classic Mode)

This mode allows you to perform a detailed comparison of two Excel files.

**Key Features**:

-   **Advanced Key Column Selection**: Interactively map columns and select a composite key.
-   **Header Normalization**: Supports multi-row and merged-cell headers.
-   **Profile Management**: Save and load complex comparison configurations.
-   **Large File Support**: Includes a streaming mode to handle large `.xlsx` files efficiently.

### 2. SPEC QA Recon (New)

This mode provides a powerful interface for filtering an Excel file based on complex, user-defined rules. The screen is divided into a **Filter Logic Builder** on the left, a **Data Viewer** on the right, and an **Action Bar** at the bottom.

**Key Features**:

-   **Robust Data Handling**: Advanced header detection for multi-row and merged-cell headers, proper handling of empty cells, and special character mapping (`'P'` -> `✓`).
-   **Sequential Multi-Cell Filtering**: Select multiple cells from the "Filter Values Preview" table, and a guided dialog flow will walk you through creating a separate, specific rule for each selection, one by one.
-   **Advanced Grouping Logic**: Create nested groups of filters and combine them with explicit `AND` or `OR` connectors that appear between each rule or group, providing a clear and intuitive way to build complex expressions.
-   **Live Match Counts**: As soon as a rule is created, the application runs a background check and displays the number of matching records, color-coded for instant feedback (green for > 0, red for 0).
-   **Dynamic Results Preview**: No more dialogs. View results for the entire filter set in a "Consolidated Results" tab, or click the "View" button on any rule/group to see its specific results in a new, closable tab with its own download/copy actions.
-   **Profile Versioning**: Saving a profile with an existing name now creates a new, versioned file (e.g., `MyProfile-v2.json`) instead of overwriting, preserving your work history.
-   **Profile Manager**: A new dialog available from the "Edit" menu allows you to view, load, and delete all saved profile versions.

## Technical Stack

-   **Language**: Java 11
-   **UI**: Java Swing with MigLayout
-   **Excel Handling**: Apache POI (including streaming APIs)
-   **Serialization**: Jackson (for profiles)
-   **Logging**: SLF4J + Logback
-   **Build Tool**: Maven with the `maven-shade-plugin`

## How to Build and Run

### Build

To compile and package the application into a single runnable "fat JAR", run:
```sh
mvn clean package
```
This creates `excel-utility-1.0.0-all.jar` in the `target` directory.

### Run

Execute the generated JAR file:
```sh
java -jar target/excel-utility-1.0.0-all.jar
```
You will be prompted to choose a mode upon startup.

### Run Tests

To run the full suite of unit tests:
```sh
mvn test
```

## Developer Notes

-   **Project Structure**: The project is organized into `gui`, `core`, `io`, `excel`, and `test` packages. The main UI is managed by `AppContainer.java`, which uses a `CardLayout` to switch between `ModeSelectionPanel`, `ComparePanel`, and `FilterPanel`.
-   **Profiles**: Comparison profiles are saved as `.json` files in a `profiles/` directory created where the application is run.
-   **Test Cases**: Generated test cases are saved to the `target/test-cases/` directory.

## Troubleshooting

If a comparison fails, the application will no longer crash. Instead, it will display a "Comparison Failed" dialog with a user-friendly error message.

If you need to report a bug, please include the full details from this dialog:
1.  Click the **"Details"** button to expand the dialog and show the full technical error message and stack trace.
2.  Click the **"Copy to Clipboard"** button.
3.  Paste the copied details into your bug report. This provides crucial information for debugging the issue.
