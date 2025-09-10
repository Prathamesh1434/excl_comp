package com.excelutility;

import com.excelutility.io.ExcelReader;
import com.excelutility.io.SimpleExcelWriter;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class SimpleExcelWriterTest {

    @Test
    void testWrite() throws Exception {
        String filePath = "target/test-files/writer-test.xlsx";
        new File(filePath).getParentFile().mkdirs();
        List<List<Object>> data = new ArrayList<>();
        data.add(List.of("Header1", "Header2"));
        data.add(List.of("Data1", 123.45));

        SimpleExcelWriter.write(data, "TestSheet", filePath);

        File file = new File(filePath);
        assert(file.exists());

        // Read back to verify content
        List<List<Object>> readData = ExcelReader.read(filePath, "TestSheet", false);
        assertNotNull(readData);
        assertEquals(2, readData.size());
        assertEquals("Header1", readData.get(0).get(0));
        assertEquals("123.45", readData.get(1).get(1).toString());

        file.delete();
    }
}
