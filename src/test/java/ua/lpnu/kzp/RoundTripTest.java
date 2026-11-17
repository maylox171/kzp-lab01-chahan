package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RoundTripTest {

    @TempDir
    Path temporaryDirectory;

    private static final List<VetVisit> ORIGINAL = List.of(
            new RoutineVisit("Барсік", "Кіт", "Іваненко Олена", LocalDate.of(2026, 3, 1),
                    "Плановий огляд", "Огляд", 450.5),
            new UrgentVisit("Рекс", "Собака", "Петренко, Олег", LocalDate.of(2026, 3, 2),
                    "Перелом лапи, \"складний\"", "Хірургія", 2500.0),
            new RoutineVisit("Кеша", "Папуга", "Іваненко Олена", LocalDate.of(2026, 3, 3),
                    "Профілактика", "Консультація", 200.0));

    @Test
    void exportEscapesAndRoundTripPreservesData() throws Exception {
        Path path = temporaryDirectory.resolve("visits.csv");
        CsvExporter.write(path, VetVisit.class, ORIGINAL);

        String csv = Files.readString(path, StandardCharsets.UTF_8);
        List<List<String>> rows = CsvParser.parseDocument(csv);
        List<VetVisit> restored = Main.readExport(path);

        assertEquals(4, rows.size());
        assertEquals("тварина,дата,діагноз,власник,вартість,вид,категорія,тип візиту",
                String.join(",", rows.get(0)));
        assertTrue(csv.contains("\"Петренко, Олег\""));
        assertTrue(csv.contains("\"Перелом лапи, \"\"складний\"\"\""));
        assertEquals(ORIGINAL, restored);
        assertEquals(ORIGINAL.get(1).key(), restored.get(1).key());
        assertEquals(UrgentVisit.class, restored.get(1).getClass());
    }

    @Test
    void roundTripPreservesMultilineField() throws Exception {
        List<VetVisit> original = List.of(new RoutineVisit("Мурчик", "Кіт", "Бондар Ірина",
                LocalDate.of(2026, 3, 5), "Перший рядок\nДругий рядок", "Вакцинація", 350.0));
        Path path = temporaryDirectory.resolve("multiline.csv");

        CsvExporter.write(path, VetVisit.class, original);
        String csv = Files.readString(path, StandardCharsets.UTF_8);

        assertTrue(csv.contains("\"Перший рядок\nДругий рядок\""));
        assertEquals(original, Main.readExport(path));
    }

    @Test
    void emptyCsvAndHeaderOnlyGiveNoVisits() throws Exception {
        Path empty = temporaryDirectory.resolve("empty.csv");
        Files.writeString(empty, "", StandardCharsets.UTF_8);
        assertTrue(Main.readExport(empty).isEmpty());

        Path headerOnly = temporaryDirectory.resolve("header.csv");
        CsvExporter.write(headerOnly, VetVisit.class, List.of());
        assertTrue(Main.readExport(headerOnly).isEmpty());
    }

    @Test
    void wrongFieldCountIsFormatError() {
        DataStorageException exception = assertThrows(DataStorageException.class,
                () -> VetVisit.fromCsv("Барсік,2026-03-01,Огляд"));
        assertTrue(exception.getMessage().contains("очікується 8 полів, знайдено 3"));
        assertNull(exception.getCause());
    }

    @Test
    void badNumberKeepsCause() {
        DataStorageException exception = assertThrows(DataStorageException.class,
                () -> VetVisit.fromCsv("Барсік,2026-03-01,Огляд,Іваненко Олена,дорого,Кіт,ROUTINE,Огляд"));
        assertInstanceOf(NumberFormatException.class, exception.getCause());
    }

    @Test
    void badEnumKeepsCause() {
        DataStorageException exception = assertThrows(DataStorageException.class,
                () -> VetVisit.fromCsv("Барсік,2026-03-01,Огляд,Іваненко Олена,10,Кіт,SOMETIMES,Огляд"));
        assertInstanceOf(IllegalArgumentException.class, exception.getCause());
        assertTrue(exception.getMessage().contains("SOMETIMES"));
    }

    @Test
    void badDateKeepsCause() {
        DataStorageException exception = assertThrows(DataStorageException.class,
                () -> VetVisit.fromCsv("Барсік,2026-13-40,Огляд,Іваненко Олена,10,Кіт,ROUTINE,Огляд"));
        assertInstanceOf(DateTimeParseException.class, exception.getCause());
    }

    @Test
    void badBooleanInInputLineIsRejected() {
        DataStorageException exception = assertThrows(DataStorageException.class,
                () -> VetVisit.fromInputLine("Пушок;Кріль;Ткачук Віра;2026-03-09;Огляд;Огляд;300.00;так"));
        assertTrue(exception.getMessage().contains("true або false"));
    }

    @Test
    void modelViolationIsWrappedWithCause() {
        DataStorageException exception = assertThrows(DataStorageException.class,
                () -> VetVisit.fromCsv("Мурка,2026-03-07,Травма,Сидоренко Марія,-150,Кіт,URGENT,Травма"));
        assertInstanceOf(IllegalArgumentException.class, exception.getCause());
        assertTrue(exception.getMessage().contains("ціна не може бути від'ємною"));
    }

    @Test
    void missingFileErrorKeepsIoCause() {
        DataStorageException exception = assertThrows(DataStorageException.class,
                () -> Main.readExport(temporaryDirectory.resolve("missing.csv")));
        assertInstanceOf(IOException.class, exception.getCause());
    }

    @Test
    void mainExportProducesBothFilesAndReportsRoundTrip() throws Exception {
        Repository<VetVisit> repository = new Repository<>();
        ORIGINAL.forEach(repository::add);

        String text = Main.exportAndVerify(repository, temporaryDirectory);

        assertTrue(text.contains("Round-trip: true"));
        assertTrue(text.contains("Відновлених об'єктів: 3"));
        List<String> machine = Files.readAllLines(
                temporaryDirectory.resolve(Main.MACHINE_FILE), StandardCharsets.UTF_8);
        assertEquals("animal,cost,date,diagnosis,owner,species,urgent,visit_type", machine.get(0));
        assertEquals(4, machine.size());
    }
}
