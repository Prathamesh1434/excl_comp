# Excel Utility - User Manual

Welcome to the Excel Utility! This guide will walk you through the features of the application and how to use them to compare your Excel files.

## 1. Main Window Overview

The main window is divided into several key sections:
-   **Menu Bar**: Access major functions like saving/loading profiles, running the comparison, and generating test cases.
-   **File Configuration Panels**: At the top, you'll find two identical panels for configuring your "Source" (left) and "Target" (right) Excel files.
-   **Column Mappings & Preview**: The central area of the application is split between the Column Mapping panel on the left and the File Preview panels on the right.
-   **Results Table & Summary**: The bottom area is split between the detailed results table on the left and a high-level summary panel on the right.
-   **Status Bar**: At the very bottom, this bar provides feedback on the current operation.

## 2. Basic Workflow

A typical workflow involves these steps:

1.  **Load Files**: In the "Source File" and "Target File" panels, click the **"Browse..."** button to select the two Excel files you want to compare.
2.  **Select Sheets**: Once a file is loaded, the "Sheet" dropdown in its panel will be populated. Select the sheet you want to compare from each file.
3.  **(Recommended) Detect Headers**: In each file panel, click the **"Detect Header"** button. This will open a dialog that automatically suggests which rows are part of the header, which is crucial for complex, multi-row headers. Review and confirm the selection.
4.  **(Optional) Preview Files**: Click the **"Preview"** button to see the first 10 rows of the selected sheet in the preview panel.
5.  **Configure Mappings**:
    -   Go to the "Column Mappings" panel. The application will use the detected headers to automatically map columns between the two files.
    -   Review the mappings and select at least one **"Is Key"** column to define how rows should be matched.
6.  **(Optional) Apply Filters**: Use the **"Filter..."** button to build a filter, or load a saved filter to narrow down the data before comparison.
7.  **Run Comparison**: Click **"Tools" > "Run Comparison"** from the menu bar.
8.  **View Results**: The results will appear in the results table and the summary panel.
9.  **Export Report**: Go to **"File" > "Export Results..."** to save the results to a two-sheet, styled Excel file.

## 3. Key Features in Detail

### Advanced Header Detection

For files with complex, multi-row, or merged headers, using the **"Detect Header"** button in each file panel is highly recommended.
-   **Automatic Detection**: The tool scans the first 20 rows of your sheet and uses heuristics (cell styling, merged regions, data types) to calculate a "confidence score" for each row.
-   **Interactive Dialog**: It then presents these rows in a dialog. Rows that it thinks are headers will be pre-selected.
-   **User Override**: You can override the automatic selection by checking or unchecking the box for any row.
-   **Concatenation Mode**: The dialog also lets you choose how to build the final column name from multiple header rows:
    -   `LEAF_ONLY`: (Default) Uses only the text from the bottom-most selected header row for that column.
    -   `BREADCRUMB`: Joins the text from all selected header rows (e.g., "Sales | Q1 | Product Name"). This is very powerful for auto-mapping.

### Column Mapping and Key Selection

This is done in the **"Column Mappings"** panel.
1.  **Auto-Mapping**: After headers are detected, the application automatically tries to map columns from source to target. It uses an exact match on the canonical header names first, then falls back to a "fuzzy match" for columns that are similar but not identical.
2.  **Manual Mapping**: For any unmapped columns, use the dropdown in the "Target Column" cell to select the correct column.
3.  **Select Key Columns**: Tick the **"Is Key"** checkbox for one or more source columns to define how rows are matched.
4.  **Ignore Columns**: Tick the **"Ignore"** checkbox to completely exclude a column from the comparison. Ignored columns will be grayed out and have a strikethrough. Use the **"Clear All Ignores"** button to reset this for all columns.
5.  **Auto-Suggest Keys**: If you're unsure which columns to use as a key, click the **"Auto-Suggest Keys"** button in either the source or target file panel. The tool will analyze that file's data for uniqueness and suggest the best key candidates.

### Filter Management

-   **Build & Apply**: Click the **"Filter..."** button in a file panel to open the Filter Builder. Create your conditions and click "Apply" to use the filter for the next comparison.
-   **Save & Load**: In the Filter Builder, you can click **"Save Filter"** to name and save your filter configuration for the current session. You can then use the **"Load"** button in the main file panel to apply a saved filter without having to rebuild it.
-   **Clear Filter**: Click the **"Clear"** button to remove any active filter on that file.

## 4. Understanding the Results

The results table and summary panel give you a complete picture of the comparison.

### Summary Panel
This panel shows high-level statistics:
-   Total rows in each file.
-   Number of identical and mismatched rows.
-   Number of rows found only in the source or only in the target.

### Results Table Color-Coding
-   **White**: Identical rows.
-   **Light Green**: Source-only rows.
-   **Light Pink**: Target-only rows.
-   **Very Light Yellow**: Rows with at least one mismatch.
-   **Yellow (Cell)**: Numeric mismatch.
-   **Pink/Coral (Cell)**: String mismatch.
-   **Orange (Cell)**: Blank vs. non-blank mismatch.
-   **Purple (Cell)**: Data type mismatch.

---
*Thank you for using Excel Utility!*
