package com.excelutility.io;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.model.SharedStringsTable;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;


import java.io.InputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads data from an Excel file, with support for streaming large XLSX files.
 */
public class ExcelReader {

    // For streaming .xlsx files
    private static class SheetHandler extends DefaultHandler {
        private final SharedStringsTable sst;
        private String lastContents;
        private boolean nextIsString;
        private List<Object> currentRow = new ArrayList<>();
        private final List<List<Object>> sheetData = new ArrayList<>();
        private int currentCol = -1;
        private int lastRowIndex = -1;

        SheetHandler(SharedStringsTable sst) {
            this.sst = sst;
        }

        public List<List<Object>> getSheetData() {
            return sheetData;
        }

        private int getColumnIndex(String cellReference) {
            if (cellReference == null) return -1;
            String col = cellReference.replaceAll("\\d", "");
            int index = 0;
            for (int i = 0; i < col.length(); i++) {
                index = index * 26 + (col.charAt(i) - 'A' + 1);
            }
            return index - 1;
        }

        public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
            if (name.equals("row")) {
                int rowIndex = Integer.parseInt(attributes.getValue("r")) - 1;
                // Handle empty rows between the last row and the current one
                for (int i = lastRowIndex + 1; i < rowIndex; i++) {
                    sheetData.add(new ArrayList<>());
                }
                lastRowIndex = rowIndex;
                currentCol = -1;
            }

            if (name.equals("c")) { // cell
                String cellReference = attributes.getValue("r");
                int thisCol = getColumnIndex(cellReference);

                // Add empty cells for any skipped columns
                for (int i = currentCol + 1; i < thisCol; i++) {
                    currentRow.add("");
                }
                currentCol = thisCol;

                String cellType = attributes.getValue("t");
                nextIsString = (cellType != null && cellType.equals("s"));
            }
            lastContents = "";
        }

        public void endElement(String uri, String localName, String name) throws SAXException {
            if (nextIsString) {
                int idx = Integer.parseInt(lastContents);
                lastContents = new XSSFRichTextString(sst.getItemAt(idx).getString()).toString();
                nextIsString = false;
            }

            if (name.equals("v")) { // value
                currentRow.add(lastContents);
            } else if (name.equals("row")) { // end of a row
                sheetData.add(new ArrayList<>(currentRow));
                currentRow.clear();
            }
        }

        public void characters(char[] ch, int start, int length) throws SAXException {
            lastContents += new String(ch, start, length);
        }
    }

    public static List<List<Object>> read(String filePath, String sheetName, boolean useStreaming) throws IOException, InvalidFormatException {
        if (useStreaming && filePath.toLowerCase().endsWith(".xlsx")) {
            try {
                return readStream(filePath, sheetName);
            } catch (Exception e) {
                throw new IOException("Streaming read failed", e);
            }
        } else {
            return readInMemory(filePath, sheetName);
        }
    }

    private static List<List<Object>> readInMemory(String filePath, String sheetName) throws IOException {
        List<List<Object>> data = new ArrayList<>();
        try (Workbook workbook = WorkbookFactory.create(new File(filePath))) {
            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                throw new IllegalArgumentException("Sheet '" + sheetName + "' not found in the workbook.");
            }
            DataFormatter dataFormatter = new DataFormatter();
            int maxCols = 0;
            for (Row row : sheet) {
                maxCols = Math.max(maxCols, row.getLastCellNum());
            }

            for (Row row : sheet) {
                List<Object> rowData = new ArrayList<>();
                for (int i = 0; i < maxCols; i++) {
                    Cell cell = row.getCell(i, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    if (cell == null) {
                        rowData.add("");
                    } else {
                        rowData.add(dataFormatter.formatCellValue(cell));
                    }
                }
                data.add(rowData);
            }
        }
        return data;
    }

    private static List<List<Object>> readStream(String filePath, String sheetName) throws Exception {
        try (OPCPackage pkg = OPCPackage.open(filePath)) {
            XSSFReader r = new XSSFReader(pkg);
            SharedStringsTable sst = (SharedStringsTable) r.getSharedStringsTable();
            XMLReader parser = XMLReaderFactory.createXMLReader();
            SheetHandler handler = new SheetHandler(sst);
            parser.setContentHandler(handler);

            XSSFReader.SheetIterator iter = (XSSFReader.SheetIterator) r.getSheetsData();
            while (iter.hasNext()) {
                try (InputStream stream = iter.next()) {
                    if (sheetName.equalsIgnoreCase(iter.getSheetName())) {
                        InputSource sheetSource = new InputSource(stream);
                        parser.parse(sheetSource);
                        List<List<Object>> sheetData = handler.getSheetData();

                        // Normalize rows to have the same number of columns
                        int maxCols = 0;
                        for (List<Object> row : sheetData) {
                            if (row.size() > maxCols) {
                                maxCols = row.size();
                            }
                        }
                        for (List<Object> row : sheetData) {
                            while (row.size() < maxCols) {
                                row.add("");
                            }
                        }
                        return sheetData;
                    }
                }
            }
        }
        throw new IllegalArgumentException("Sheet '" + sheetName + "' not found in the workbook.");
    }

    public static List<String> getSheetNames(String filePath) throws IOException {
        List<String> sheetNames = new ArrayList<>();
        try (Workbook workbook = WorkbookFactory.create(new File(filePath))) {
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                sheetNames.add(workbook.getSheetName(i));
            }
        }
        return sheetNames;
    }

    public static List<List<Object>> readPreview(String filePath, String sheetName, int rowLimit) throws IOException {
        List<List<Object>> data = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = WorkbookFactory.create(fis)) {
            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                throw new IOException("Sheet '" + sheetName + "' not found in the workbook.");
            }

            int lastRow = Math.min(sheet.getLastRowNum(), rowLimit - 1);
            if (lastRow < 0) return data; // Empty sheet

            int maxCols = 0;
            for (int i = 0; i <= lastRow; i++) {
                Row row = sheet.getRow(i);
                if (row != null) {
                    maxCols = Math.max(maxCols, row.getLastCellNum());
                }
            }

            for (int i = 0; i <= lastRow; i++) {
                Row row = sheet.getRow(i);
                List<Object> rowData = new ArrayList<>();
                for (int j = 0; j < maxCols; j++) {
                    if (row == null) {
                        rowData.add("");
                        continue;
                    }
                    Cell cell = row.getCell(j, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    if (cell == null) {
                        rowData.add("");
                        continue;
                    }
                    switch (cell.getCellType()) {
                        case STRING:
                            rowData.add(cell.getStringCellValue());
                            break;
                        case NUMERIC:
                            if (DateUtil.isCellDateFormatted(cell)) {
                                rowData.add(cell.getDateCellValue().toString());
                            } else {
                                rowData.add(new DataFormatter().formatCellValue(cell));
                            }
                            break;
                        case BOOLEAN:
                            rowData.add(cell.getBooleanCellValue());
                            break;
                        case FORMULA:
                             try {
                                rowData.add(new DataFormatter().formatCellValue(cell, workbook.getCreationHelper().createFormulaEvaluator()));
                            } catch (Exception e) {
                                rowData.add("!FORMULA_ERROR!");
                            }
                            break;
                        case BLANK:
                            rowData.add("");
                            break;
                        default:
                            rowData.add("!UNSUPPORTED_TYPE!");
                    }
                }
                data.add(rowData);
            }
        }
        return data;
    }
}
