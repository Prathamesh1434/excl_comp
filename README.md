# Excel Comparator

An interactive Java Swing desktop application to compare two Excel files. This tool supports multi-row header normalization, dynamic key column mapping, and generates detailed, color-coded comparison reports.

## Features

-   **Compare Excel Files**: Supports both `.xls` and `.xlsx` formats.
-   **Dynamic Key Mapping**: Define one or more pairs of columns (one from each file) to be used as a composite key for row alignment.
-   **Data Preview**: Shows a preview of the first 10 rows of data from each selected sheet.
-   **Header Normalization**: Supports multi-row headers by merging them into a single, combined header. This can be enabled per file, and the number of header rows is configurable.
-   **Flexible Comparison Options**:
    -   Enable/disable column-level comparison.
    -   Enable/disable detection of missing/extra rows.
-   **Color-Coded Results**: Displays comparison results in a table with clear color-coding:
    -   **Yellow**: Mismatched rows.
    -   **Pink**: Rows missing in one of the files.
    -   **Orange**: Specific cells within a mismatched row that have different values.
-   **Tooltip on Mismatched Cells**: Hover over a mismatched cell to see the values from both files.
-   **Export to Excel**: Export the full comparison report to a new Excel file, preserving the color-coding for easy analysis and sharing.

## Technical Stack

-   **Language**: Java 11+
-   **UI**: Java Swing
-   **Excel Handling**: Apache POI
-   **Build Tool**: Maven

## Installation

1.  **Clone the repository**:
    ```sh
    git clone <repository-url>
    cd excel-comparator
    ```

2.  **Prerequisites**:
    -   Java 11 or higher must be installed.
    -   Apache Maven must be installed.

## How to Run

### Development Mode

To run the application from the source code:

1.  **Compile the code**:
    ```sh
    mvn compile
    ```

2.  **Run the application**:
    ```sh
    mvn exec:java
    ```

### Packaged JAR

To build a runnable JAR file with all dependencies included:

1.  **Package the application**:
    ```sh
    mvn package
    ```

2.  **Run the JAR file**:
    The packaged JAR will be located in the `target` directory.
    ```sh
    java -jar target/excel-comparator-1.0-SNAPSHOT-jar-with-dependencies.jar
    ```

## UI Layout

The application window is divided into several sections:
-   **Top**: Normalization options for each file.
-   **Middle**: A side-by-side preview of the loaded Excel files.
-   **Bottom-Left**: Comparison options, including checkboxes and the dynamic key mapping UI.
-   **Bottom-Right**: The results table where the comparison report is displayed.
-   **Bottom**: Action buttons to run the comparison, export, clear, and exit.

*(A screenshot of the application would be placed here to illustrate the layout.)*
