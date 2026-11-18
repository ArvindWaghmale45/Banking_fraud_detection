package com.fraud.backend.service;

import com.fraud.backend.exception.InvalidDatasetException;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CsvDatasetInspectorTests {

    private static final String HEADER = "Time,"
            + IntStream.rangeClosed(1, 28).mapToObj(index -> "V" + index).collect(Collectors.joining(","))
            + ",Amount,Class";

    @Test
    void countsRowsWithoutCountingHeaderOrQuotedNewlinesAsExtraRecords() throws Exception {
        String csv = HEADER + "\r\n"
                + "0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,12.50,0\r\n"
                + "\"1\ncontinued\",1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,9.25,1";

        assertEquals(2, CsvDatasetInspector.countRecords(new StringReader(csv)));
    }

    @Test
    void rejectsWrongHeaders() {
        assertThrows(
                InvalidDatasetException.class,
                () -> CsvDatasetInspector.countRecords(new StringReader("time,amount,class\n"))
        );
    }

    @Test
    void rejectsUnterminatedQuotedField() {
        assertThrows(
                InvalidDatasetException.class,
                () -> CsvDatasetInspector.countRecords(new StringReader(HEADER + "\n\"unfinished"))
        );
    }

    @Test
    void rejectsRowsWithDifferentColumnCounts() {
        assertThrows(
                InvalidDatasetException.class,
                () -> CsvDatasetInspector.countRecords(new StringReader(HEADER + "\nonly,two"))
        );
    }
}
