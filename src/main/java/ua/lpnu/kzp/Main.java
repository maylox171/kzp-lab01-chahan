package ua.lpnu.kzp;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Точка входу застосунку ветклініки: читає вхідний файл, будує звіт,
 * експортує дані у CSV та перевіряє round-trip.
 */
public final class Main {

    /** Типовий шлях до вхідного файлу. */
    public static final Path DEFAULT_INPUT = Path.of("data", "input.csv");

    /** Типовий каталог для згенерованих CSV-файлів. */
    public static final Path DEFAULT_OUTPUT_DIR = Path.of("target", "data");

    /** Назва CSV-експорту предметної моделі з українськими заголовками. */
    public static final String EXPORT_FILE = "vet_visits_export.csv";

    /** Назва машинного CSV для Python-блоку. */
    public static final String MACHINE_FILE = "vet_visits.csv";

    private Main() {
        // Забороняє створення екземплярів класу точки входу
    }

    /**
     * Завантажує коректні рядки вхідного файлу у сховище, а помилки — у список.
     *
     * @param lines      рядки вхідного файлу
     * @param repository сховище для коректних візитів
     * @param errors     список для повідомлень про помилки
     */
    public static void loadInto(List<String> lines, Repository<VetVisit> repository,
            List<String> errors) {
        Objects.requireNonNull(lines, "Рядки не можуть бути null");
        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index);
            if (line.isBlank()) {
                continue;
            }
            try {
                repository.add(VetVisit.fromInputLine(line));
            } catch (DataStorageException e) {
                // Хибний рядок не зупиняє обробку: повідомлення з номером рядка потрапляє у звіт
                errors.add("Рядок %d: %s".formatted(index + 1, e.getMessage()));
            }
        }
    }

    /**
     * Обробляє список вхідних рядків, створює об'єкти візитів,
     * збирає статистику та формує звіт для тестів і консолі.
     *
     * @param lines рядки вхідного файлу
     * @return текст звіту
     */
    public static String processLines(List<String> lines) {
        Repository<VetVisit> repository = new Repository<>();
        List<String> errorMessages = new ArrayList<>();
        loadInto(lines, repository, errorMessages);
        return formatReport(repository.all(), errorMessages);
    }

    /**
     * Формує звіт з показниками лабораторної роботи № 1.
     *
     * @param visits коректні візити
     * @param errors повідомлення про помилки
     * @return текст звіту
     */
    public static String formatReport(List<VetVisit> visits, List<String> errors) {
        double totalPrice = 0.0;
        int urgentCount = 0;
        for (VetVisit visit : visits) {
            totalPrice += visit.getPrice();
            if (visit.isUrgent()) {
                urgentCount++;
            }
        }
        double averagePrice = visits.isEmpty() ? 0.0 : totalPrice / visits.size();

        StringBuilder reportBuilder = new StringBuilder();
        reportBuilder.append(String.format(Locale.ROOT, "Кількість коректних записів: %d%n", visits.size()));
        reportBuilder.append(String.format(Locale.ROOT, "Загальний виторг: %.2f%n", totalPrice));
        reportBuilder.append(String.format(Locale.ROOT, "Середня вартість візиту: %.2f%n", averagePrice));
        reportBuilder.append(String.format(Locale.ROOT, "Кількість ургентних візитів: %d%n", urgentCount));
        reportBuilder.append(String.format(Locale.ROOT, "Помилок: %d%n", errors.size()));
        for (String err : errors) {
            reportBuilder.append(err).append(System.lineSeparator());
        }
        return reportBuilder.toString();
    }

    /**
     * Читає CSV-експорт назад у список візитів.
     *
     * @param path шлях до CSV-файлу експорту
     * @return відновлені візити в порядку рядків файлу
     * @throws DataStorageException якщо файл неможливо прочитати або рядок хибний
     */
    public static List<VetVisit> readExport(Path path) throws DataStorageException {
        String csv;
        try {
            csv = Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new DataStorageException("Не вдалося прочитати CSV: %s".formatted(path), exception);
        }
        List<List<String>> rows = CsvParser.parseDocument(csv);
        List<VetVisit> restored = new ArrayList<>();
        // Перший запис — заголовок, тому відновлення починається з другого
        for (int index = 1; index < rows.size(); index++) {
            try {
                restored.add(VetVisit.fromFields(rows.get(index)));
            } catch (DataStorageException exception) {
                throw new DataStorageException(
                        "Запис %d файлу %s: %s".formatted(index + 1, path, exception.getMessage()),
                        exception);
            }
        }
        return List.copyOf(restored);
    }

    /**
     * Експортує сховище у CSV, читає файл назад і формує звіт про round-trip.
     *
     * @param repository сховище візитів
     * @param outputDir  каталог для CSV-файлів
     * @return текст звіту round-trip
     * @throws DataStorageException якщо експорт або імпорт неможливий
     */
    public static String exportAndVerify(Repository<VetVisit> repository, Path outputDir)
            throws DataStorageException {
        Path exportPath = outputDir.resolve(EXPORT_FILE);
        Path machinePath = outputDir.resolve(MACHINE_FILE);
        List<VetVisit> original = repository.all();

        CsvExporter.write(exportPath, VetVisit.class, original);
        CsvExporter.write(machinePath, VetVisitRecord.class,
                original.stream().map(VetVisitRecord::new).toList());

        String csv;
        try {
            csv = Files.readString(exportPath, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new DataStorageException("Не вдалося прочитати CSV: %s".formatted(exportPath), exception);
        }
        List<List<String>> rows = CsvParser.parseDocument(csv);
        List<VetVisit> restored = readExport(exportPath);

        StringBuilder text = new StringBuilder();
        text.append(String.format(Locale.ROOT, "CSV-експорт: %s%n", exportPath));
        text.append(String.format(Locale.ROOT, "Записів у сховищі: %d%n", original.size()));
        text.append(String.format(Locale.ROOT, "CSV-записів із заголовком: %d%n", rows.size()));
        text.append(String.format(Locale.ROOT, "Відновлених об'єктів: %d%n", restored.size()));
        text.append(String.format(Locale.ROOT, "Заголовок: %s%n",
                rows.isEmpty() ? "" : String.join(",", rows.get(0))));
        text.append(String.format(Locale.ROOT, "Екранування коми або лапок: %s%n",
                csv.contains("\"")));
        text.append(String.format(Locale.ROOT, "Round-trip: %s%n", original.equals(restored)));
        if (!original.isEmpty()) {
            VetVisit first = original.get(0);
            boolean keyFound = restored.stream().anyMatch(v -> v.key().equals(first.key()));
            text.append(String.format(Locale.ROOT, "Ключ round-trip (власник + дата) %s / %s: %s%n",
                    first.getOwner(), first.getDate(), keyFound ? "відновлено" : "втрачено"));
        }
        text.append(String.format(Locale.ROOT, "Машинний CSV для Python: %s%n", machinePath));
        return text.toString();
    }

    /**
     * Запускає застосунок.
     *
     * @param args необов'язкові аргументи: шлях до вхідного файлу та каталог для CSV
     */
    public static void main(String[] args) {
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(new FileOutputStream(FileDescriptor.err), true, StandardCharsets.UTF_8));

        Path inputPath = args.length > 0 ? Path.of(args[0]) : DEFAULT_INPUT;
        Path outputDir = args.length > 1 ? Path.of(args[1]) : DEFAULT_OUTPUT_DIR;
        if (!Files.exists(inputPath)) {
            System.err.println("Файл " + inputPath + " не знайдено!");
            return;
        }

        try {
            List<String> lines = Files.readAllLines(inputPath, StandardCharsets.UTF_8);
            Repository<VetVisit> repository = new Repository<>();
            List<String> errors = new ArrayList<>();
            loadInto(lines, repository, errors);
            System.out.println(formatReport(repository.all(), errors));
            System.out.print(exportAndVerify(repository, outputDir));
        } catch (IOException exception) {
            System.err.println("Помилка читання файлу: " + exception.getMessage());
        } catch (DataStorageException exception) {
            System.err.println("Помилка зберігання даних: " + exception.getMessage());
            if (exception.getCause() != null) {
                System.err.println("Причина: " + exception.getCause());
            }
        }
    }
}
