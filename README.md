# Excel Utility

A production-ready, GUI-only Java Swing desktop application for comparing two Excel files. This tool provides a comprehensive set of features for normalization, key mapping, detailed comparison, and reporting, all configurable through an intuitive user interface.

## High-Level Features

-   **Full GUI Control**: No command-line interface or manual file editing required. All configurations, from file loading to complex normalization rules, are managed through the GUI.
-   **Advanced Comparison Engine**: Supports various row matching strategies (primary key, row order, fuzzy matching) and detailed cell-by-cell comparison with configurable tolerances.
-   **Normalization Pipeline**: Apply a series of transformations (e.g., trim, case change, regex replace) to data before comparison, with rules configurable globally or per-column.
-   **Profile Management**: Save and load complex comparison configurations to and from profiles via a user-friendly Profile Manager.
-   **Rich Reporting**: Export comparison results to color-coded Excel files, HTML reports, or CSV summaries.
-   **Test Case Generator**: A built-in tool to automatically generate a suite of test files for various scenarios, helping to validate the tool's functionality.
-   **Large File Support**: Includes a streaming mode to handle large `.xlsx` files efficiently without consuming excessive memory.

## Technical Stack

-   **Language**: Java 11
-   **UI**: Java Swing with MigLayout for a modern, flexible layout.
-   **Excel Handling**: Apache POI, including its streaming APIs.
-   **Serialization**: Jackson for saving/loading profiles in JSON format.
-   **Logging**: SLF4J + Logback.
-   **Fuzzy Matching**: Apache Commons Text.
-   **Build Tool**: Maven with the `maven-shade-plugin` for creating a runnable fat JAR.

## Installation

1.  **Prerequisites**:
    -   Java 11 or higher must be installed and configured.
    -   Apache Maven must be installed.

2.  **Clone the repository**:
    ```sh
    git clone <repository-url>
    cd excel-utility
    ```

## How to Build and Run

### Build

To compile the source code and package the application into a single runnable "fat JAR", run the following Maven command from the project root:

```sh
mvn clean package
```

This will create a file named `excel-utility-1.0.0-all.jar` in the `target` directory.

### Run

To run the application, execute the generated JAR file:

```sh
java -jar target/excel-utility-1.0.0-all.jar
```

### Run Tests

To run the suite of unit and integration tests, use the following Maven command:

```sh
mvn test
```

## Developer Notes

-   **Project Structure**: The project is organized into several packages:
    -   `com.excelutility.gui`: Contains all Swing UI components.
    -   `com.excelutility.core`: Contains the headless comparison engine and data models.
    -   `com.excelutility.io`: Handles file I/O, including the `ExcelReader` and `ProfileService`.
    -   `com.excelutility.test`: Contains the `TestCaseGenerator` logic.
-   **Design Pattern**: The application follows a separation of concerns, with the core logic being completely independent of the Swing GUI, which makes the engine highly testable.
-   **Profiles**: Comparison profiles are saved as `.json` files in a `profiles/` directory created in the same location where the application is run.
-   **Test Cases**: Generated test cases are saved to the `target/test-cases/` directory.
