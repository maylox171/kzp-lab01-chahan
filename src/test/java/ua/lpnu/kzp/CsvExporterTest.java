package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CsvExporterTest {

    @TempDir
    Path temporaryDirectory;

    /* Базовий клас стороннього домену для перевірки успадкованих полів. */
    static class Base {
        @CsvColumn("Код")
        private final String code;

        Base(String code) {
            this.code = code;
        }
    }

    /* Сторонній клас, про який експортер нічого не знає. */
    static final class Book extends Base {
        @CsvColumn("Назва")
        private final String title;

        @CsvColumn("Рік")
        private final int year;

        private final String notExported = "приховано";

        Book(String code, String title, int year) {
            super(code);
            this.title = title;
            this.year = year;
        }
    }

    /* Клас без анотованих полів. */
    static final class Plain {
        private final String value = "x";
    }

    @Test
    void annotationIsAvailableAtRuntimeOnFields() throws NoSuchFieldException {
        Retention retention = CsvColumn.class.getAnnotation(Retention.class);
        Target target = CsvColumn.class.getAnnotation(Target.class);
        assertEquals(RetentionPolicy.RUNTIME, retention.value());
        assertEquals(List.of(ElementType.FIELD), List.of(target.value()));

        Field owner = VetVisitRecord.class.getDeclaredField("owner");
        assertNotNull(owner.getAnnotation(CsvColumn.class));
        assertEquals("власник", owner.getAnnotation(CsvColumn.class).value());
    }

    @Test
    void headerIsSortedByJavaFieldName() throws DataStorageException {
        // animal, cost, date, diagnosis, owner, species
        assertEquals(List.of("тварина", "вартість", "дата", "діагноз", "власник", "вид"),
                CsvExporter.headers(VetVisitRecord.class));
    }

    @Test
    void exporterWorksWithAnyClassIncludingSuperclassFields() throws DataStorageException {
        String csv = CsvExporter.toCsv(Book.class, List.of(new Book("A-01", "Карти, що говорять", 1988)));
        List<String> lines = csv.lines().toList();
        assertEquals("Код,Назва,Рік", lines.get(0));
        assertEquals("A-01,\"Карти, що говорять\",1988", lines.get(1));
        assertFalse(csv.contains("приховано"));
    }

    @Test
    void escapesCommaQuotesAndLineBreaks() {
        assertEquals("просто", CsvExporter.escape("просто"));
        assertEquals("\"a,b\"", CsvExporter.escape("a,b"));
        assertEquals("\"сказав \"\"так\"\"\"", CsvExporter.escape("сказав \"так\""));
        assertEquals("\"1\n2\"", CsvExporter.escape("1\n2"));
        assertEquals("", CsvExporter.escape(null));
    }

    @Test
    void emptyListWritesHeaderOnly() throws Exception {
        Path path = temporaryDirectory.resolve("empty.csv");
        CsvExporter.write(path, VetVisitRecord.class, List.of());
        List<List<String>> rows = CsvParser.parseDocument(Files.readString(path, StandardCharsets.UTF_8));
        assertEquals(1, rows.size());
        assertEquals(CsvExporter.headers(VetVisitRecord.class), rows.get(0));
    }

    @Test
    void classWithoutColumnsIsRejected() {
        DataStorageException exception = assertThrows(DataStorageException.class,
                () -> CsvExporter.toCsv(Plain.class, List.of(new Plain())));
        assertTrue(exception.getMessage().contains("не має CSV-полів"));
    }

    @Test
    void wrapsFileErrorWithCause() throws Exception {
        Path directory = temporaryDirectory.resolve("not-a-file");
        Files.createDirectory(directory);

        DataStorageException exception = assertThrows(DataStorageException.class,
                () -> CsvExporter.write(directory, VetVisitRecord.class, List.of()));

        assertInstanceOf(IOException.class, exception.getCause());
        assertTrue(exception.getMessage().contains("Не вдалося записати CSV"));
    }

    @Test
    void rewritingTruncatesOldContent() throws Exception {
        Path path = temporaryDirectory.resolve("visits.csv");
        LocalDate day = LocalDate.of(2026, 3, 1);
        CsvExporter.write(path, VetVisitRecord.class, List.of(
                new VetVisitRecord("Барсік", "Кіт", "Іваненко Олена", day, "Огляд", 1.0),
                new VetVisitRecord("Кеша", "Папуга", "Іваненко Олена", day, "Огляд", 2.0)));
        CsvExporter.write(path, VetVisitRecord.class, List.of());
        assertEquals(1, Files.readAllLines(path, StandardCharsets.UTF_8).size());
    }
}
