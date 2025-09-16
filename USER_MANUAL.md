# Excel Utility - User Manual

Welcome to the Excel Utility! This guide will walk you through how to use the **SPEC QA Recon** mode to easily filter your Excel data.

## 1. Getting Started

When you first launch the application, you will see a "Mode Selection" screen.

1.  Click the **"SPEC QA Recon"** button to enter the filtering mode.
2.  This will take you to the main filtering interface.

## 2. Loading Your Data

The first step is to tell the tool which Excel file you want to filter.

1.  In the top-left corner, find the **"Data File (to be filtered)"** section.
2.  Click the **"Browse..."** button. A file selection window will open.
3.  Navigate to your Excel file (ending in `.xls` or `.xlsx`) and click **"Open"**.
4.  Once you select a file, the **"Sheet"** dropdown menu below it will fill with all the sheets from your workbook. Select the sheet you want to work with.
5.  Click the **"Load Previews"** button at the bottom-left of the screen. This will load a sample of your data into the "Data Preview" pane on the right, so you can see what you're working with.

## 3. Checking the Headers

Headers are the titles of your columns (e.g., "First Name", "Sale Amount", "Date"). It's important that the tool knows which row contains these titles.

1.  After selecting a sheet, click the **"Detect Header"** button.
2.  A dialog will appear with a list of rows from your sheet. The tool will automatically check the boxes for the rows it thinks are your headers.
3.  If the automatic selection is correct, simply click **"OK"**.
4.  If the selection is incorrect, you can manually check or uncheck the boxes to correctly identify your header row(s). Then click **"OK"**.

## 4. Building Your Filter

Now you can create rules to filter your data. The filter logic is built in the **"Filter Logic Builder"** on the left side of the screen.

#### Adding a Simple Rule

Let's say you want to find all rows where the "Country" column is "USA".

1.  Click the **"Add Rule"** button inside the main "Root" group.
2.  A new panel for your rule will appear.
3.  In the first dropdown, select the column you want to filter on (e.g., "Country").
4.  In the second dropdown, select the comparison you want to make (e.g., "equals").
5.  In the text box, type the value you want to look for (e.g., "USA").

As soon as you create the rule, a green or red number will appear next to it, telling you how many rows in your data match that single rule.

#### Adding More Complex Rules

You can combine rules with "AND" or "OR":

*   **AND**: Means all conditions must be true. (e.g., Country is "USA" AND Status is "Shipped")
*   **OR**: Means any of the conditions can be true. (e.g., Country is "USA" OR Country is "Canada")

After you add your first rule, a connector will appear below it. You can click it to toggle between **AND** and **OR**. You can then click **"Add Rule"** again to add another rule to the same group.

#### Using Groups

You can create nested groups for even more complex logic (e.g., (Country is "USA" AND Status is "Shipped") OR (Country is "Canada" AND Status is "Pending")).

1.  Click the **"Add Group"** button.
2.  A new colored group panel will appear. You can give it a name.
3.  You can now add rules and even other groups inside this new group, just like you did before.

## 5. Viewing Your Results

Once you have built your filter expression, you can see the results.

1.  Click the **"Run Filter"** button in the bottom action bar.
2.  The application will process your entire file.
3.  A dialog will pop up asking you which columns you want to see in the results. Select the columns you are interested in and click **"OK"**.
4.  The final, filtered data will appear in the **"Result Preview"** panel at the bottom of the screen.

If you want to save these results to a new Excel file, click the **"Download Results"** button instead.

## 6. Saving and Loading Your Work

If you create a complex filter that you want to use again later, you can save it as a "Profile".

#### Saving a Profile

1.  Go to the **File** menu in the top-left corner of the window.
2.  Click **"Save Profile As..."**.
3.  Enter a name for your profile (e.g., "Q3 Sales Report Filter") and click **"OK"**.
4.  Your profile, including the filter logic and the path to the file you were using, is now saved.

#### Loading a Profile

1.  Go to the **File** menu.
2.  Click **"Load Profile..."**.
3.  Select the profile you want to load from the list and click **"OK"**.
4.  The application will automatically load your saved file path, sheet selection, and the entire filter expression you built, ready for you to use again.
