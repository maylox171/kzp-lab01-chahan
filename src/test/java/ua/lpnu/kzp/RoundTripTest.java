package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RoundTripTest {

    @TempDir
    Path temporaryDirectory;

    private static final List<VetVisitRecord> ORIGINAL = List.of(
            new VetVisitRecord("Барсік", "Кіт", "Іваненко Олена", LocalDate.of(2026, 3, 1),
                    "Плановий огляд", 450.5),
            new VetVisitRecord("Рекс", "Собака", "Петренко, Олег", LocalDate.of(2026, 3, 2),
                    "Перелом лапи, \"складний\"", 2500.0),
            new VetVisitRecord("Кеша", "Папуга", "Іваненко Олена", LocalDate.of(2026, 3, 3),
                    "Профілактика", 200.0),
            new VetVisitRecord("Джек", "Собака", "Коваль Андрій", LocalDate.of(2026, 3, 4),
                    "Отруєння", 1000.0),
            new VetVisitRecord("Ласка", "Тхір", "Коваль Андрій", LocalDate.of(2026, 3, 6),
                    "Травма лапи", 1200.0));

    @Test
    void exportEscapesAndRoundTripPreservesData() throws Exception {
        Path path = temporaryDirectory.resolve("visits.csv");
        CsvExporter.write(path, VetVisitRecord.class, ORIGINAL);

        String csv = Files.readString(path, StandardCharsets.UTF_8);
        List<List<String>> rows = CsvParser.parseDocument(csv);
        List<VetVisitRecord> restored = VetVisitRecord.readCsv(path);

        assertEquals(6, rows.size());
        assertEquals("тварина,вартість,дата,діагноз,власник,вид", String.join(",", rows.get(0)));
        assertTrue(csv.contains("\"Петренко, Олег\""));
        assertTrue(csv.contains("\"Перелом лапи, \"\"складний\"\"\""));
        assertEquals(ORIGINAL, restored);
        assertEquals("Петренко, Олег|2026-03-02", restored.get(1).key());
    }

    @Test
    void roundTripPreservesMultilineField() throws Exception {
        List<VetVisitRecord> original = List.of(new VetVisitRecord("Мурчик", "Кіт", "Бондар Ірина",
                LocalDate.of(2026, 3, 5), "Перший рядок\nДругий рядок", 350.0));
        Path path = temporaryDirectory.resolve("multiline.csv");

        CsvExporter.write(path, VetVisitRecord.class, original);
        String csv = Files.readString(path, StandardCharsets.UTF_8);

        assertTrue(csv.contains("\"Перший рядок\nДругий рядок\""));
        assertEquals(original, VetVisitRecord.readCsv(path));
    }

    @Test
    void equalsAndHashCodeUseAllFields() {
        Set<VetVisitRecord> set = new HashSet<>(ORIGINAL);
        set.add(new VetVisitRecord("Барсік", "Кіт", "Іваненко Олена", LocalDate.of(2026, 3, 1),
                "Плановий огляд", 450.5));
        assertEquals(ORIGINAL.size(), set.size());
        assertNotEquals(ORIGINAL.get(0), new VetVisitRecord("Барсік", "Кіт", "Іваненко Олена",
                LocalDate.of(2026, 3, 1), "Плановий огляд", 451.0));
    }

    @Test
    void emptyCsvAndHeaderOnlyGiveNoRecords() throws Exception {
        Path empty = temporaryDirectory.resolve("empty.csv");
        Files.writeString(empty, "", StandardCharsets.UTF_8);
        assertTrue(VetVisitRecord.readCsv(empty).isEmpty());

        Path headerOnly = temporaryDirectory.resolve("header.csv");
        CsvExporter.write(headerOnly, VetVisitRecord.class, List.of());
        assertTrue(VetVisitRecord.readCsv(headerOnly).isEmpty());
    }

    @Test
    void wrongFieldCountIsFormatError() {
        DataStorageException exception = assertThrows(DataStorageException.class,
                () -> VetVisitRecord.fromCsv("Барсік,450.5,2026-03-01"));
        assertTrue(exception.getMessage().contains("очікується 6 полів, знайдено 3"));
        assertNull(exception.getCause());
    }

    @Test
    void badNumberKeepsCause() {
        DataStorageException exception = assertThrows(DataStorageException.class,
                () -> VetVisitRecord.fromCsv("Рижик,дорого,2026-03-08,Огляд,Мельник Петро,Кіт"));
        assertInstanceOf(NumberFormatException.class, exception.getCause());
    }

    @Test
    void badDateKeepsCause() {
        DataStorageException exception = assertThrows(DataStorageException.class,
                () -> VetVisitRecord.fromCsv("Бім,300,2026-13-40,Огляд,Лисенко Ганна,Собака"));
        assertInstanceOf(DateTimeParseException.class, exception.getCause());
    }

    @Test
    void modelViolationIsWrappedWithCause() {
        DataStorageException negative = assertThrows(DataStorageException.class,
                () -> VetVisitRecord.fromCsv("Мурка,-150,2026-03-07,Травма,Сидоренко Марія,Кіт"));
        assertInstanceOf(IllegalArgumentException.class, negative.getCause());
        assertTrue(negative.getMessage().contains("вартість не може бути від'ємною"));

        DataStorageException blank = assertThrows(DataStorageException.class,
                () -> VetVisitRecord.fromCsv("Мурка,10,2026-03-07,Травма, ,Кіт"));
        assertInstanceOf(IllegalArgumentException.class, blank.getCause());
    }

    @Test
    void badRecordInFileReportsRecordNumber() throws Exception {
        Path path = temporaryDirectory.resolve("bad.csv");
        Files.writeString(path, "тварина,вартість,дата,діагноз,власник,вид\n"
                + "Барсік,450.5,2026-03-01,Огляд,Іваненко Олена,Кіт\n"
                + "Рижик,дорого,2026-03-08,Огляд,Мельник Петро,Кіт\n", StandardCharsets.UTF_8);
        DataStorageException exception = assertThrows(DataStorageException.class,
                () -> VetVisitRecord.readCsv(path));
        assertTrue(exception.getMessage().contains("Запис 3"));
        assertInstanceOf(DataStorageException.class, exception.getCause());
    }

    @Test
    void missingFileErrorKeepsIoCause() {
        DataStorageException exception = assertThrows(DataStorageException.class,
                () -> VetVisitRecord.readCsv(temporaryDirectory.resolve("missing.csv")));
        assertInstanceOf(IOException.class, exception.getCause());
    }

    @Test
    void mainExportReportsRoundTrip() throws Exception {
        Repository<VetVisitRecord> repository = new Repository<>();
        ORIGINAL.forEach(repository::add);

        String text = Main.exportAndVerify(repository, temporaryDirectory);

        assertTrue(text.contains("Round-trip: true"));
        assertTrue(text.contains("Відновлених об'єктів: 5"));
        assertTrue(text.contains("ключ Петренко, Олег / 2026-03-02: відновлено"));
    }

    @Test
    void projectDataFilesLoad() throws Exception {
        assertEquals(6, Main.loadRepository(Path.of("data", "vet_visits.csv")).size());
        assertEquals(0, Main.loadRepository(Path.of("data", "vet_visits_empty.csv")).size());
        assertThrows(DataStorageException.class,
                () -> Main.loadRepository(Path.of("data", "vet_visits_bad.csv")));
    }
}
