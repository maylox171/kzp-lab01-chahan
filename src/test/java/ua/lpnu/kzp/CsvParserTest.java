package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class CsvParserTest {

    @Test
    void parsesQuotedCommaAndDoubledQuotes() throws DataStorageException {
        List<String> fields = CsvParser.parse("Рекс,\"Петренко, Олег\",\"Перелом \"\"складний\"\"\"");
        assertEquals(List.of("Рекс", "Петренко, Олег", "Перелом \"складний\""), fields);
    }

    @Test
    void parsesMultilineFieldAndBothLineSeparators() throws DataStorageException {
        String text = "a,b\r\n1,\"рядок 1\nрядок 2\"\n2,x";
        List<List<String>> rows = CsvParser.parseDocument(text);
        assertEquals(3, rows.size());
        assertEquals("рядок 1\nрядок 2", rows.get(1).get(1));
        assertEquals(List.of("2", "x"), rows.get(2));
    }

    @Test
    void supportsSemicolonDelimiterAndEmptyFields() throws DataStorageException {
        assertEquals(List.of("a", "", "b,c"), CsvParser.parse("a;;b,c", ';'));
    }

    @Test
    void emptyDocumentGivesNoRecords() throws DataStorageException {
        assertTrue(CsvParser.parseDocument("").isEmpty());
    }

    @Test
    void rejectsBrokenQuotesAndNull() {
        assertThrows(DataStorageException.class, () -> CsvParser.parseDocument("\"незакрите"));
        assertThrows(DataStorageException.class, () -> CsvParser.parseDocument("\"a\"b,c"));
        assertThrows(DataStorageException.class, () -> CsvParser.parseDocument(null));
        assertThrows(DataStorageException.class, () -> CsvParser.parse("a\nb"));
    }
}
