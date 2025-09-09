# Excel Utility

An interactive Java Swing desktop application to compare two Excel files. This tool supports multi-row headers, composite key matching, and generates color-coded comparison reports.

## Features

-   **Compare Excel Files**: Supports both `.xls` and `.xlsx` formats.
-   **Sheet Selection**: Allows you to select which sheet to compare from each file.
-   **Data Preview**: Shows a preview of the first 10 rows of data from each selected sheet.
-   **Composite Key Matching**: Define one or more columns as a composite key to match rows between the two files.
-   **Color-Coded Results**: Displays comparison results in a table with color-coding for identical, mismatched, and missing rows.
-   **Tooltip on Mismatched Cells**: Hover over a mismatched cell to see the values from both files.
-   **Export to Excel**: Export the comparison report to a new Excel file, preserving the color-coding.
-   **Header Normalization**: Supports multi-row headers by merging them into a single header.

## Technical Stack

-   **Language**: Java 11+
-   **UI**: Java Swing
-   **Excel Handling**: Apache POI
-   **Build Tool**: Maven

## Installation

1.  **Clone the repository**:
    ```sh
    git clone <repository-url>
    cd excel-utility
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

## GUI Screenshots

*(Here you would include screenshots of the application's GUI, showing the file selection, preview panels, key selection, and results table.)*

**Main Window:**
`[Screenshot of the main application window]`

**File Selection:**
`[Screenshot of the file chooser dialog]`

**Results Display:**
`[Screenshot of the results table with color-coding]`

## Example Output

The exported Excel file (`comparison_result.xlsx`) will look like this:

-   **White rows**: Identical rows.
-   **Yellow rows**: Mismatched rows.
-   **Pink rows**: Rows that are missing in one of the files.
-   **Orange cells**: Specific cells within a mismatched row that have different values.

`[Screenshot of the output Excel file]`
