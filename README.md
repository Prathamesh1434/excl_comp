# Excel Utility

A production-ready, GUI-only Java Swing desktop application for comparing two Excel files. This tool provides a comprehensive set of features for normalization, key mapping, detailed comparison, and reporting, all configurable through an intuitive user interface.

## Key Features

-   **GUI-Only Configuration**: No command-line interface or manual file editing required. All configurations, from file loading to complex normalization rules, are managed through the GUI.
-   **Advanced Key Column Selection**:
    -   An interactive table allows you to map columns from the source file to the target file.
    -   Select which mappings to use as a **composite key** by ticking an "Is Key" checkbox.
    -   A dedicated panel shows you exactly which columns are currently selected as keys.
    -   An **"Auto-Suggest Keys"** feature analyzes your data and recommends the best columns to use as unique identifiers.
-   **Header Normalization**: Supports multi-row headers by merging them into a single, combined header. This can be enabled per file, and the number of header rows is configurable.
-   **Profile Management**: Save and load complex comparison configurations to and from profiles via a user-friendly Profile Manager.
-   **Test Case Generator**: A built-in tool to automatically generate a suite of test files for various scenarios.
-   **Large File Support**: Includes a streaming mode to handle large `.xlsx` files efficiently.

*(Note: Advanced features like the Filter Builder, detailed normalization pipelines, and rich exports are planned but not yet fully implemented in this version.)*

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

### Run Tests

To run the full suite of unit tests:
```sh
mvn test
```

## Developer Notes

-   **Project Structure**: The project is organized into `gui`, `core`, `io`, `excel`, and `test` packages.
-   **Profiles**: Profiles are saved as `.json` files in a `profiles/` directory created where the application is run.
-   **Test Cases**: Generated test cases are saved to the `target/test-cases/` directory.
