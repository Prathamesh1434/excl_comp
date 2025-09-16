# Excel Utility - User Manual

Welcome to the Excel Utility! This guide will walk you through the features of the application.

## 1. Choosing a Mode

When you first start the application, you will be asked to choose a mode. You can switch between modes at any time by using the "File" > "Back to Mode Selection" menu item.

-   **Compare Excel Files**: Use this mode to perform a detailed, cell-by-cell comparison of two Excel files.
-   **SPEC QA Recon**: Use this mode to filter a main data file based on values from a second file using a powerful logic builder.

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

## 3. SPEC QA Recon Mode

This mode provides a powerful interface for filtering an Excel file based on complex, user-defined rules. The screen is divided into a left-hand control panel and a right-hand data view area, with a main action bar at the bottom.

### Basic Workflow

1.  **Load Files**: At the top of the screen, use the **"Browse..."** buttons to select your main **Data File** and the **Filter Values File**. After selecting a file, choose the correct sheet from the dropdown menu for each.

2.  **Preview Data**: Click the **"Load & Preview Files"** button. This will load the data from your selected sheets into the tables in the right-hand panel. You can switch between the **"Data Preview"** and **"Filter Values Preview"** tabs to inspect the data.

3.  **Define Header Rows (If Necessary)**: If your files have complex, multi-row headers, click the **"Detect Header"** button for each file to ensure the application correctly identifies the column titles before you begin building filters.

4.  **Build Your Filter Logic**:
    -   The **"Filter Logic Builder"** on the left is where you will construct your filter. It starts with a single "root" group.
    -   **Adding a Rule**: In the **"Filter Values Preview"** tab, find a cell containing a value you want to filter by and **double-click** it. This will add a new filter rule to the currently selected group. You can also use the **"Add Rule"** button inside any group.
    -   **Adding a Group**: Click the **"Add Group"** button inside any existing group to create a nested group for more complex `(A and B) or C` style logic.
    -   **Setting Logic**: Each group has its own **AND | OR radio buttons**. Use these to control whether all conditions (`AND`) or any condition (`OR`) within that group must be met.

5.  **Run Filters and View Results**:
    -   Use the **Action Bar** at the bottom of the window to execute your filter.
    -   **Run Filter**: Click this to run the complete filter expression against your Data File. The results will appear in the **"Results"** tab.
    -   **Download Results**: Click this to run the filter and save the results directly to an Excel file.
    -   **Exit**: Closes the application.

### Profile Management

You can save and load your filter configurations using the **File Menu**.

-   **File > Save Profile As...**: Saves the current set of rules and a reference to the selected sheet name. If a profile with the same name exists, a new, versioned file (e.g., `MyProfile-v2.json`) will be created automatically.
-   **File > Load Profile...**: Opens a dropdown allowing you to select and load a previously saved profile.
-   **Edit > Manage Profiles...**: Opens a dialog where you can view, load, or delete any of your saved profiles, including different versions.

---
*Thank you for using Excel Utility!*
