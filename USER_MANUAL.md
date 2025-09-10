# Excel Utility - User Manual

Welcome to the Excel Utility! This guide will walk you through the features of the application and how to use them to compare your Excel files.

## 1. Main Window Overview

The main window is divided into several key sections:
-   **Menu Bar**: Access major functions like saving/loading profiles, running the comparison, and generating test cases.
-   **File Configuration Panels**: At the top, you'll find two identical panels for configuring your "Source" (left) and "Target" (right) Excel files.
-   **Column Mappings & Preview**: The central area of the application is split between the Column Mapping panel on the left and the File Preview panels on the right.
-   **Results Table**: The bottom area where the comparison results will be displayed.
-   **Status Bar**: At the very bottom, this bar provides feedback on the current operation.

## 2. Basic Workflow

A typical workflow involves these steps:

1.  **Load Files**: In the "Source File" and "Target File" panels, click the **"Browse..."** button to select the two Excel files you want to compare. You can also paste a file path directly into the text field.
2.  **Select Sheets**: Once a file is loaded, the "Sheet" dropdown in its panel will be populated. Select the sheet you want to compare from each file.
3.  **(Optional) Preview Files**: Click the **"Preview"** button next to a file path to see the first 10 rows of the selected sheet in the preview panel on the right. This is useful for verifying you have the correct file and sheet.
4.  **Configure Comparison**:
    -   Go to the "Column Mappings" panel to define which columns should be used to match rows.
    -   (Optional) Configure header options in the file panels.
5.  **Run Comparison**: Click **"Tools" > "Run Comparison"** from the menu bar.
6.  **View Results**: The results will appear in the results table, with detailed color-coding to highlight differences.
7.  **Export Report**: Go to **"File" > "Export Results..."** to save the results to a styled Excel file.

## 3. Key Features in Detail

### Column Mapping and Key Selection

This is the most important configuration step. It is done in the **"Column Mappings"** panel.

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
-   Select one or more scenarios to create (e.g., "Exact Match").
-   Choose the number of rows to generate and click "Generate".
-   The files will be created in a `target/test-cases` directory inside the application folder.

## 4. Understanding the Results

The results table uses a detailed color-coding scheme for both rows and individual cells.

### Row Colors:
-   **White**: The rows were matched by key and all corresponding cells were identical.
-   **Light Green**: This row was found only in the **Source** file.
-   **Light Pink**: This row was found only in the **Target** file.
-   **Very Light Yellow**: The rows were matched by key, but one or more cells had different values. The specific mismatched cells will be highlighted with a more intense color.

### Cell Colors (for mismatched rows):
-   **Yellow**: A mismatch between two **numeric** values.
-   **Pink/Coral**: A mismatch between two **string** values.
-   **Orange**: One cell was **blank** while the other was not.
-   **Purple**: The cells contained different **data types** (e.g., a number was compared to a string).

Hover your mouse over any highlighted cell for a tooltip showing the exact values from the source and target files.

---
*Thank you for using Excel Utility!*
