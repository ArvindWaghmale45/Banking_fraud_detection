package com.fraud.backend.service;

import com.fraud.backend.exception.InvalidDatasetException;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class CsvDatasetInspector {

    private static final Set<String> REQUIRED_COLUMNS = buildRequiredColumns();

    private CsvDatasetInspector() {
    }

    static long countRecords(Reader source) throws IOException {
        try (PushbackReader reader = new PushbackReader(source, 1)) {
            List<String> headers = new ArrayList<>();
            StringBuilder field = new StringBuilder();
            boolean inQuotes = false;
            boolean afterQuote = false;
            boolean atFieldStart = true;
            boolean headerRead = false;
            boolean recordHasContent = false;
            long records = 0;
            int rowFields = 1;
            int current;

            while ((current = reader.read()) != -1) {
                char character = (char) current;

                if (inQuotes) {
                    if (character == '"') {
                        int next = reader.read();
                        if (next == '"') {
                            if (!headerRead) {
                                field.append('"');
                            }
                        } else {
                            inQuotes = false;
                            afterQuote = true;
                            if (next != -1) {
                                reader.unread(next);
                            }
                        }
                    } else if (!headerRead) {
                        field.append(character);
                    }
                    recordHasContent = true;
                    continue;
                }

                if (afterQuote && character != ',' && character != '\r' && character != '\n') {
                    throw new InvalidDatasetException("CSV contains unexpected characters after a quoted field.");
                }

                if (character == '"') {
                    if (!atFieldStart || afterQuote) {
                        throw new InvalidDatasetException("CSV contains an invalid quote in a field.");
                    }
                    inQuotes = true;
                    atFieldStart = false;
                    recordHasContent = true;
                } else if (character == ',') {
                    if (!headerRead) {
                        headers.add(field.toString());
                    }
                    field.setLength(0);
                    atFieldStart = true;
                    afterQuote = false;
                    recordHasContent = true;
                    rowFields++;
                } else if (character == '\r' || character == '\n') {
                    if (character == '\r') {
                        int next = reader.read();
                        if (next != '\n' && next != -1) {
                            reader.unread(next);
                        }
                    }
                    if (!headerRead) {
                        headers.add(field.toString());
                        validateHeaders(headers);
                        headerRead = true;
                    } else if (recordHasContent) {
                        validateRowLength(rowFields, headers.size());
                        records++;
                    }
                    field.setLength(0);
                    atFieldStart = true;
                    afterQuote = false;
                    recordHasContent = false;
                    rowFields = 1;
                } else {
                    if (afterQuote) {
                        throw new InvalidDatasetException("CSV contains unexpected characters after a quoted field.");
                    }
                    if (!headerRead) {
                        field.append(character);
                    }
                    atFieldStart = false;
                    if (!Character.isWhitespace(character)) {
                        recordHasContent = true;
                    }
                }
            }

            if (inQuotes) {
                throw new InvalidDatasetException("CSV contains an unterminated quoted field.");
            }
            if (!headerRead) {
                headers.add(field.toString());
                validateHeaders(headers);
                headerRead = true;
            } else if (recordHasContent) {
                validateRowLength(rowFields, headers.size());
                records++;
            }
            return records;
        }
    }

    private static void validateRowLength(int fields, int expectedFields) {
        if (fields != expectedFields) {
            throw new InvalidDatasetException("CSV rows must have the same columns as the header.");
        }
    }

    private static void validateHeaders(List<String> headers) {
        if (!headers.isEmpty() && headers.get(0).startsWith("\uFEFF")) {
            headers.set(0, headers.get(0).substring(1));
        }

        Set<String> normalized = new HashSet<>();
        for (String header : headers) {
            normalized.add(header.trim().toLowerCase(java.util.Locale.ROOT));
        }
        if (headers.size() != REQUIRED_COLUMNS.size() || !normalized.equals(REQUIRED_COLUMNS)) {
            throw new InvalidDatasetException(
                    "CSV headers must contain Time, V1-V28, Amount, and Class columns."
            );
        }
    }

    private static Set<String> buildRequiredColumns() {
        Set<String> columns = new HashSet<>();
        columns.add("time");
        columns.add("amount");
        columns.add("class");
        for (int index = 1; index <= 28; index++) {
            columns.add("v" + index);
        }
        return Set.copyOf(columns);
    }
}
