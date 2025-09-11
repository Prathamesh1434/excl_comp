# Excel Utility - User Manual

Welcome to the Excel Utility! This guide will walk you through the features of the application.

## 1. Choosing a Mode

When you first start the application, you will be asked to choose a mode. You can switch between modes at any time by using the "File" > "Back to Mode Selection" menu item.

-   **Compare Excel Files**: Use this mode to perform a detailed, cell-by-cell comparison of two Excel files.
-   **Filter Excel Data**: Use this mode to filter a main data file based on values from a second file.

---

## 2. Compare Excel Files Mode

This mode allows you to perform a detailed comparison of two Excel files.

### Basic Workflow

1.  **Load Files**: In the "Source File" and "Target File" panels, click the **"Browse..."** button to select the two Excel files you want to compare.
2.  **Select Sheets**: Once a file is loaded, the "Sheet" dropdown in its panel will be populated. Select the sheet you want to compare from each file.
3.  **(Recommended) Detect Headers**: In each file panel, click the **"Detect Header"** button. This will open a dialog that automatically suggests which rows are part of the header, which is crucial for complex, multi-row headers.
4.  **Configure Mappings**: Go to the "Column Mappings" panel to map columns and select at least one **"Is Key"** column to define how rows should be matched.
5.  **Run Comparison**: Click **"Tools" > "Run Comparison"** from the menu bar.
6.  **View Results**: The results will appear in the results table and the summary panel.
7.  **Export Report**: Go to **"File" > "Export Results..."** to save the results.

*(For more details on advanced features like Profile Management and Test Case Generation, please refer to the README file.)*

---

## 3. Filter Excel Data Mode

This mode allows you to filter one Excel file (the "Data File") using values from another Excel file (the "Filter Values File").

### Basic Workflow

1.  **Load Files**:
    -   In the **"Data File"** panel, click "Browse..." to select the main Excel file you want to filter.
    -   In the **"Filter Values File"** panel, click "Browse..." to select the Excel file that contains the values you want to use for filtering.

2.  **Select and Preview Sheets**:
    -   For each file, select the correct sheet from the dropdown. Use the "Search Sheet" box to quickly find a sheet in large workbooks.
    -   Click the **"Load & Preview Files"** button to load the data into the preview tables. The preview tables have grid lines and can be sorted by clicking on the column headers.

3.  **Define Header Rows (If Necessary)**:
    -   If your files have complex, multi-row headers, click the **"Detect Header"** button for each file to correctly identify the header rows before creating filters.

4.  **Create Filter Rules**:
    -   In the "Filter Values Preview" table, find a cell you want to use for filtering and **double-click** it (or select one or more cells and click "Add Filter from Selection").
    -   A dialog will appear. Choose how you want to filter:
        -   **Filter by Value**: Uses the exact value of the cell you clicked (e.g., "Yes").
        -   **Filter using Column Name**: Uses the header of the column you clicked as the filter value (e.g., "Female").
    -   A second dialog will appear. Select the column(s) from your **Data File** that you want to apply this filter to. You can also choose whether to "Trim whitespace" for more flexible matching.
    -   The new filter rule will appear in the "Configured Filters" list. Repeat this step to add as many rules as you need.

5.  **Apply Filters and Download**:
    -   In the action panel, choose your **Filter Logic**:
        -   **AND**: A row in your Data File will only be included if it matches ALL of the rules you created.
        -   **OR**: A row will be included if it matches AT LEAST ONE of the rules you created.
    -   (Optional) Click **"Set Highlight Color"** to choose a color for the matching rows in the output file.
    -   Click **"Download Filtered Results"**. This will apply your filter rules and prompt you to save the resulting Excel file.

### Understanding the Filter Panel

-   **Data File Panel**: Where you load the main file to be filtered.
-   **Filter Values File Panel**: Where you load the file containing the values to use as filters.
-   **Data Preview**: Shows a preview of your main data file.
-   **Filter Values Preview**: Shows the full content of your filter values file. This is the table you interact with to create filters.
-   **Configured Filters**: A list of all the filter rules you have created. Use the "Clear All Filters" button to start over.
-   **Action Panel**: Contains all the controls for creating and applying filters, setting options, and downloading the final report.

---
*Thank you for using Excel Utility!*
