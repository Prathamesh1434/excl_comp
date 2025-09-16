package com.excelutility.test;

import com.excelutility.core.CanonicalNameBuilder;
import com.excelutility.core.ConcatenationMode;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CanonicalNameBuilderTest {

    private static File testExcelFile;

    @BeforeAll
    public static void setup(@TempDir Path tempDir) throws IOException {
        testExcelFile = tempDir.resolve("test_headers.xlsx").toFile();
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("TestSheet");

            // Row 0: Grand Parent Headers
            Row row0 = sheet.createRow(0);
            row0.createCell(0).setCellValue("GrandParent1");
            row0.createCell(2).setCellValue("GrandParent2");

            // Row 1: Parent Headers
            Row row1 = sheet.createRow(1);
            row1.createCell(0).setCellValue("Parent1");
            row1.createCell(1).setCellValue("Parent2");
            row1.createCell(2).setCellValue("Parent3");
            row1.createCell(4).setCellValue("Parent4");


            // Row 2: Child Headers (Leaf nodes)
            Row row2 = sheet.createRow(2);
            row2.createCell(0).setCellValue("Child1");
            row2.createCell(1).setCellValue("Child2");
            row2.createCell(2).setCellValue("Child3");
            row2.createCell(3).setCellValue("Child4");
            row2.createCell(4).setCellValue("Child5");
            row2.createCell(5).setCellValue("Child6");

            // Create merged regions to simulate complex headers
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 1)); // GrandParent1 spans 2 columns
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 2, 4)); // GrandParent2 spans 3 columns
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 2, 3)); // Parent3 spans 2 columns


            try (FileOutputStream fos = new FileOutputStream(testExcelFile)) {
                workbook.write(fos);
            }
        }
    }

    @Test
    void testBuildCanonicalHeaders_BreadcrumbMode() throws IOException, org.apache.poi.openxml4j.exceptions.InvalidFormatException {
        try (Workbook workbook = new XSSFWorkbook(testExcelFile)) {
            Sheet sheet = workbook.getSheet("TestSheet");
            List<Integer> headerRows = List.of(0, 1, 2);
            List<String> expectedHeaders = List.of(
                    "GrandParent1 | Parent1 | Child1",
                    "GrandParent1 | Parent2 | Child2",
                    "GrandParent2 | Parent3 | Child3",
                    "GrandParent2 | Parent3 | Child4",
                    "GrandParent2 | Parent4 | Child5",
                    "Column 6"
            );

            List<String> actualHeaders = CanonicalNameBuilder.buildCanonicalHeaders(sheet, headerRows, ConcatenationMode.BREADCRUMB, " | ");

            assertEquals(expectedHeaders, actualHeaders);
        }
    }

    @Test
    void testBuildCanonicalHeaders_LeafOnlyMode() throws IOException, org.apache.poi.openxml4j.exceptions.InvalidFormatException {
        try (Workbook workbook = new XSSFWorkbook(testExcelFile)) {
            Sheet sheet = workbook.getSheet("TestSheet");
            List<Integer> headerRows = List.of(0, 1, 2);
            List<String> expectedHeaders = List.of(
                    "Child1",
                    "Child2",
                    "Child3",
                    "Child4",
                    "Child5",
                    "Child6"
            );

            List<String> actualHeaders = CanonicalNameBuilder.buildCanonicalHeaders(sheet, headerRows, ConcatenationMode.LEAF_ONLY, " | ");
            // The 6th column has a header in row 2, so it should not be a default name.
            // Let's adjust the expectation.
            List<String> finalExpected = List.of("Child1", "Child2", "Child3", "Child4", "Child5", "Child6");
            assertEquals(finalExpected, actualHeaders);
        }
    }
}
