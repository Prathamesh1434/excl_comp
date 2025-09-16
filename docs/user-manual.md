# Excel Utility - User Manual

Welcome to the Excel Utility! This guide will walk you through the features of the application.

## 1. Choosing a Mode

When you first start the application, you will be asked to choose a mode. You can switch between modes at any time by using the **File > Back to Mode Selection** menu item.

-   **Compare Excel Files**: Use this mode to perform a detailed, cell-by-cell comparison of two Excel sheets.
-   **SPEC QA Recon**: Use this mode to filter a main data file based on values from another file using a powerful logic builder.

---

## 2. Compare Excel Files Mode

This mode allows you to perform a detailed comparison of two Excel files.

### Basic Workflow

1.  **Load Files**: In the "Source File" and "Target File" panels, click the **"Browse..."** button to select the two Excel files you want to compare.
2.  **Select Sheets**: Once a file is loaded, the "Sheet" dropdown in its panel will be populated. Select the sheet you want to compare from each file.
3.  **(Recommended) Detect Headers**: In each file panel, click the **"Detect Header"** button. This will open a dialog that automatically suggests which rows are part of the header, which is crucial for complex, multi-row headers.
4.  **Configure Mappings**: Go to the "Column Mappings" panel to map columns and select at least one **"Is Key"** column to define how rows should be matched.
5.  **Run Comparison**: Click **Tools > Run Comparison** from the menu bar.
6.  **View Results**: The results will appear in the results table and the summary panel.
7.  **Export Report**: Go to **File > Export Results...** to save the results.

*(For more details on advanced features like Profile Management and Test Case Generation, please refer to the README file.)*

---

## 3. SPEC QA Recon Mode

This mode provides a powerful interface for filtering an Excel file based on complex, user-defined rules. The screen is divided into a **Filter Logic Builder** on the left, a **Data Viewer** on the right, and an **Action Bar** at the bottom.

### Basic Workflow

1.  **Load Files**: At the top of the screen, use the **"Browse..."** buttons to select your main **Data File** and the **Filter Values File**. After selecting a file, choose the correct sheet from the dropdown menu for each.

2.  **Preview Data**: Click the **"Load Previews"** button in the bottom action bar. This will load the data from your selected sheets into the tables in the right-hand panel. You can switch between the **"Data Preview"** and **"Filter Values Preview"** tabs to inspect the data.

3.  **Define Header Rows (If Necessary)**: If your files have complex, multi-row headers, click the **"Detect Header"** button for each file to ensure the application correctly identifies the column titles before you begin building filters.

4.  **Build Your Filter Logic**:
    -   The **"Filter Logic Builder"** on the left is where you will construct your filter. It starts with a single "Root" group.
    -   **Add a Group**: You can add nested groups using the "Add Group" button inside any existing group. This is useful for creating complex expressions like `(A AND B) OR C`.
    -   **Add a Rule from Selection**: This is the primary way to create rules. First, select one or more cells in the **"Filter Values Preview"** table on the right. Then, click the **"Add Filter from Selection"** button located just below that table. This will begin a sequential process:
        1.  For the *first* selected cell, a dialog will ask you to choose whether to filter by the specific **cell value** or the **column name**.
        2.  A final dialog will ask you to map this to one or more **target columns** in the **Data File**.
        3.  The application will then repeat this process for the *next* selected cell, allowing you to create and configure rules one by one. You can cancel the sequence at any time.
    -   **Setting Logic**: Once a group contains more than one rule or subgroup, a connector will appear between them with **AND | OR** radio buttons. Use these to control how each item in the group is combined with the next.
    -   **Live Counts**: As soon as a rule is created, a live count of matching records will appear next to it, colored green for > 0 matches and red for 0.

#### Viewing Partial Results

In addition to running the entire filter expression, you can inspect the results of individual rules or groups:

-   **View Button**: Next to each rule and group, there is a **"View"** button.
-   **Dynamic Result Tabs**: Clicking this button will run a filter for *only that specific item* and display the results in a new, closable tab in the **Result Preview** panel at the bottom of the screen. This is extremely useful for debugging your logic, allowing you to see exactly which rows are being matched by a specific part of your filter expression without running the whole thing. Each dynamic tab also includes "Download" and "Copy to Clipboard" buttons.

5.  **Run Filters and View Results**:
    -   Use the **Action Bar** at the bottom of the window to execute your filter.
    -   **Run Filter**: Click this to run the complete filter expression against your Data File. The results will appear in the **"Consolidated Results"** tab in the Result Preview panel.
    -   **Download Results**: Click this to run the filter and save the results directly to an Excel file. A dialog will prompt you to select the columns to include in the export.
    -   **Exit**: Closes the application.

### Profile Management

You can save and load your filter configurations using the **File Menu**.

-   **File > Save Profile As...**: Saves the current set of rules, file paths, and sheet selections. If a profile with the same name exists, a new, versioned file (e.g., `MyProfile-v2.json`) will be created automatically.
-   **File > Load Profile...**: Opens a dropdown allowing you to select and load a previously saved profile.
-   **Edit > Manage Profiles...**: Opens a dialog where you can view, load, or delete any of your saved profiles, including different versions. If you load a profile that was saved with different column names than the currently loaded file, a warning will be displayed.

---
*Thank you for using Excel Utility!*
