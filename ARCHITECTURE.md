# Application Architecture

This document provides a high-level overview of the application architecture for the "SPEC QA Recon" feature, illustrated with Mermaid diagrams.

## Data Flow: Preview Search

This diagram shows the sequence of events when a user initiates a "Preview Search" to test their filter expression.

```mermaid
flowchart TD
    subgraph User Interaction
        A[User clicks 'Preview Search' button]
    end

    subgraph UI Layer (com.excelutility.gui)
        B[FilterPanel]
        C[FilterExpressionBuilderPanel]
        D[JTabbedPane 'Unified Data View']
    end

    subgraph Core Logic Layer (com.excelutility.core)
        E[FilteringService]
    end

    subgraph I/O Layer (com.excelutility.io)
        F[ExcelReader]
    end

    A --> B
    B -->|1. Gets filter logic| C
    C -->|2. Returns FilterExpression object| B
    B -->|3. Calls filter() in background| E
    E -->|4. Reads .xlsx file| F
    F -->|5. Returns row data| E
    E -->|6. Returns filtered results| B
    B -->|7. Populates 'Preview' tab| D
```

## Data Flow: Save Profile

This diagram illustrates how a user's filter configuration is persisted to the filesystem as a JSON file.

```mermaid
flowchart TD
    subgraph User Interaction
        A[User clicks 'Save Profile As...' menu item]
    end

    subgraph UI Layer (com.excelutility.gui)
        B[FilterPanel]
        C[JOptionPane for Profile Name]
    end

    subgraph Data Model (com.excelutility.core)
        D[new FilterProfile object]
    end

    subgraph I/O Service Layer (com.excelutility.io)
        E[FilterProfileService]
        F[Jackson ObjectMapper]
    end

    subgraph Filesystem
        G[profiles/MyProfile.json]
    end

    A --> B
    B -->|1. Prompts for name| C
    C -->|2. Returns profile name| B
    B -->|3. Gathers state & creates object| D
    B -->|4. Calls saveProfile()| E
    E -->|5. Serializes object to JSON| F
    F -->|6. Writes JSON string| E
    E -->|7. Saves file to disk| G
    E -->|8. Returns success| B
    B -->|9. Shows success message| C
```
