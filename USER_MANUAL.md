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

### Profile Management

-   **Saving a Profile**: After you have configured your file paths, sheet selections, key mappings, and normalization rules, you can save these settings as a profile. Go to **File > Save Profile As...** and provide a name. This saves you from having to re-configure everything for repeated tasks.
-   **Loading a Profile**: To load a previously saved set of configurations, go to **File > Load Profile...** and select a profile from the list. The entire UI will update with the saved settings.
-   **Profile Manager**: Go to **Edit > Profile Manager...** to view and delete your saved profiles.

### Test Case Generator

This tool helps you create sample Excel files to test the functionality of the comparator itself.
-   Go to **Tools > Generate Test Cases...**.
-   In the dialog, select one or more scenarios you want to create (e.g., "Exact Match", "Numeric Mismatch").
-   Choose the number of rows to generate.
-   Click "Generate". The files will be created in a `target/test-cases` directory inside the application folder.
-   The dialog will show a list of the generated test cases, which you can then load into the main application to see how the comparison works.

## 4. Understanding the Results

The results table uses a simple color-coding scheme:
-   **White Rows**: The rows were matched by key and all corresponding cells were identical.
-   **Yellow Rows**: The rows were matched by key, but one or more cells had different values.
-   **Orange Cells**: In a yellow row, these are the specific cells that contain a mismatch. Hover your mouse over them for a tooltip showing the different values.
-   **Pink Rows**: These rows were present in one file but could not be matched by key in the other file. The status column will indicate whether it was missing from the source or the target.

---
*Thank you for using Excel Utility!*
