# Excel Utility - User Manual

Welcome to the Excel Utility! This guide will walk you through the features of the application and how to use them to compare your Excel files.

## 1. Main Window Overview

The main window is divided into several key sections:
-   **Menu Bar**: Access all major functions like opening files, managing profiles, and generating test cases.
-   **File Selection Panels**: At the top, you'll find two identical panels for loading your "Source" (left) and "Target" (right) Excel files.
-   **Column Mappings & Row Matching**: The bottom-left panel where you define how to align rows between the two files.
-   **Normalization & Rules**: The bottom-right panel where you can set up data cleaning and transformation rules.
-   **Results Table**: The central area where the comparison results will be displayed.
-   **Status Bar**: At the very bottom, this bar provides feedback on the current operation.

## 2. Basic Workflow

A typical workflow involves these steps:

1.  **Load Files**: Use the "File" > "Open Source File" and "Open Target File" menu items (or the "..." buttons) to select the two Excel files you want to compare.
2.  **Select Sheets**: Once a file is loaded, the "Sheet" dropdown in its panel will be populated. Select the sheet you want to compare from each file.
3.  **Configure Comparison**:
    -   Go to the "Column Mappings & Row Matching" panel to define which columns should be used to match rows.
    -   (Optional) Go to the "Normalization & Rules" panel to set up any data cleaning rules.
4.  **Run Comparison**: Click the "Run Comparison" button from the "Tools" menu.
5.  **View Results**: The results will appear in the main results table, with color-coding to highlight differences.
6.  **Export Report**: Go to "File" > "Export Report" to save the results to an Excel, HTML, or CSV file.

## 3. Key Features in Detail

### Column Mapping and Key Selection

This is the most important configuration step. It is done in the **"Column Mappings & Row Matching"** panel.

1.  **Load both your source and target files.** The table in this panel will automatically populate with the columns from your source file.
2.  **Map Columns**: For each source column, use the dropdown in the "Target Column" cell to select the corresponding column from the target file. The application will try to auto-map columns with the same name.
3.  **Select Key Columns**: To tell the application how to match rows, you must select at least one key column. Do this by **ticking the "Is Key" checkbox** for each row that should be part of the composite key.
4.  **View Selected Keys**: The panel on the right, "Selected Keys," will show a clear list of the columns you have chosen as keys.
5.  **Auto-Suggest Keys**: If you're unsure which columns to use as a key, click the **"Auto-Suggest Keys"** button in the "Source File" panel. The tool will analyze your source data and show you a ranked list of the most unique columns, which are the best candidates for a key.

### Profile Management

-   **Saving a Profile**: After you have configured everything (files, sheets, keys, etc.), you can save these settings as a profile. Go to **File > Save Profile As...** and provide a name.
-   **Loading a Profile**: To load a previously saved set of configurations, go to **File > Load Profile...** and select a profile from the list.
-   **Profile Manager**: Go to **Edit > Profile Manager...** to view and delete your saved profiles.

### Test Case Generator

This tool helps you create sample Excel files to test the application's features.
-   Go to **Tools > Generate Test Cases...**.
-   Select one or more scenarios to create (e.g., "Exact Match", "Multi-row Headers").
-   Choose the number of rows to generate and click "Generate".
-   The files will be created in a `target/test-cases` directory inside the application folder.

## 4. Understanding the Results

The results table uses a simple color-coding scheme:
-   **White Rows**: The rows were matched by key and all corresponding cells were identical.
-   **Yellow Rows**: The rows were matched by key, but one or more cells had different values.
-   **Orange Cells**: In a yellow row, these are the specific cells that contain a mismatch. Hover your mouse over them for a tooltip showing the different values.
-   **Pink Rows**: These rows were present in one file but could not be matched by key in the other file. The status column will indicate whether it was missing from the source or the target.

---
*Thank you for using Excel Utility!*
