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

/**
 * Точка входу застосунку ветклініки: звіт лабораторних робіт № 1–4 та
 * збереження записів {@link VetVisitRecord} у CSV з перевіркою round-trip (ЛР5).
 */
public final class Main {

    /** Вхідний файл звіту ЛР1–ЛР4 (роздільник {@code ;}). */
    public static final Path DEFAULT_INPUT = Path.of("data", "input.csv");

    /** Вхідний CSV записів візитів для ЛР5. */
    public static final Path DEFAULT_RECORDS = Path.of("data", "vet_visits.csv");

    /** CSV із хибним рядком для демонстрації обробки помилки. */
    public static final Path DEFAULT_BAD_RECORDS = Path.of("data", "vet_visits_bad.csv");

    /** Каталог для згенерованих CSV-файлів. */
    public static final Path DEFAULT_OUTPUT_DIR = Path.of("target", "data");

    /** Назва файлу CSV-експорту. */
    public static final String EXPORT_FILE = "vet_visits_export.csv";

    private Main() {
        // Забороняє створення екземплярів класу точки входу
    }

    /**
     * Обробляє список вхідних рядків CSV, створює об'єкти візитів,
     * збирає статистику та формує звіт для тестів і консолі.
     *
     * @param lines рядки вхідного файлу
     * @return текст звіту
     */
    public static String processLines(List<String> lines) {
        List<VetVisit> visits = new ArrayList<>();
        List<String> errorMessages = new ArrayList<>();

        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index);
            if (line.isBlank()) {
                continue;
            }
            try {
                VetVisit visit = VetVisit.fromCsv(line);
                visits.add(visit);
            } catch (IllegalArgumentException e) {
                // Зберігаємо текст помилки для звіту
                errorMessages.add("Рядок %d: %s".formatted(index + 1, e.getMessage()));
            }
        }

        double totalPrice = 0.0;
        int urgentCount = 0;

        for (VetVisit visit : visits) {
            totalPrice += visit.getPrice();
            if (visit.isUrgent()) {
                urgentCount++;
            }
        }

        double averagePrice = visits.isEmpty() ? 0.0 : totalPrice / visits.size();

        // Формуємо загальний рядок звіту, який перевіряє MainTest
        StringBuilder reportBuilder = new StringBuilder();
        reportBuilder.append(String.format(Locale.ROOT, "Кількість коректних записів: %d%n", visits.size()));
        reportBuilder.append(String.format(Locale.ROOT, "Загальний виторг: %.2f%n", totalPrice));
        reportBuilder.append(String.format(Locale.ROOT, "Середня вартість візиту: %.2f%n", averagePrice));
        reportBuilder.append(String.format(Locale.ROOT, "Кількість ургентних візитів: %d%n", urgentCount));
        reportBuilder.append(String.format(Locale.ROOT, "Помилок: %d%n", errorMessages.size()));

        for (String err : errorMessages) {
            reportBuilder.append(err).append(System.lineSeparator());
        }

        return reportBuilder.toString();
    }

    /**
     * Завантажує записи з CSV у параметризоване сховище.
     *
     * @param path шлях до CSV-файлу з заголовком
     * @return сховище із записами у порядку файлу
     * @throws DataStorageException якщо файл неможливо прочитати або запис хибний
     */
    public static Repository<VetVisitRecord> loadRepository(Path path) throws DataStorageException {
        Repository<VetVisitRecord> repository = new Repository<>();
        VetVisitRecord.readCsv(path).forEach(repository::add);
        return repository;
    }

    /**
     * Експортує сховище у CSV, читає файл назад і формує звіт про round-trip.
     *
     * @param repository сховище записів
     * @param outputDir  каталог для CSV-файлу
     * @return текст звіту round-trip
     * @throws DataStorageException якщо експорт або імпорт неможливий
     */
    public static String exportAndVerify(Repository<VetVisitRecord> repository, Path outputDir)
            throws DataStorageException {
        Path exportPath = outputDir.resolve(EXPORT_FILE);
        List<VetVisitRecord> original = repository.all();
        CsvExporter.write(exportPath, VetVisitRecord.class, original);

        String csv;
        try {
            csv = Files.readString(exportPath, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new DataStorageException("Не вдалося прочитати CSV: %s".formatted(exportPath), exception);
        }
        List<List<String>> rows = CsvParser.parseDocument(csv);
        List<VetVisitRecord> restored = VetVisitRecord.readCsv(exportPath);

        StringBuilder text = new StringBuilder();
        text.append(String.format(Locale.ROOT, "CSV-експорт: %s%n", exportPath));
        text.append(String.format(Locale.ROOT, "Записів у сховищі: %d%n", original.size()));
        text.append(String.format(Locale.ROOT, "CSV-записів із заголовком: %d%n", rows.size()));
        text.append(String.format(Locale.ROOT, "Відновлених об'єктів: %d%n", restored.size()));
        text.append(String.format(Locale.ROOT, "Заголовок: %s%n",
                rows.isEmpty() ? "" : String.join(",", rows.get(0))));
        text.append(String.format(Locale.ROOT, "Екранування коми або лапок: %s%n", csv.contains("\"")));
        text.append(String.format(Locale.ROOT, "Round-trip: %s%n", original.equals(restored)));
        for (VetVisitRecord record : original) {
            boolean found = restored.stream().anyMatch(r -> r.key().equals(record.key()) && r.equals(record));
            text.append(String.format(Locale.ROOT, "  ключ %s / %s: %s%n",
                    record.owner(), record.date(), found ? "відновлено" : "втрачено"));
        }
        return text.toString();
    }

    /**
     * Запускає застосунок.
     *
     * @param args необов'язкові аргументи: вхідний файл ЛР1–ЛР4, CSV записів, каталог виводу
     */
    public static void main(String[] args) {
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(new FileOutputStream(FileDescriptor.err), true, StandardCharsets.UTF_8));

        Path inputPath = args.length > 0 ? Path.of(args[0]) : DEFAULT_INPUT;
        Path recordsPath = args.length > 1 ? Path.of(args[1]) : DEFAULT_RECORDS;
        Path outputDir = args.length > 2 ? Path.of(args[2]) : DEFAULT_OUTPUT_DIR;

        System.out.println("=== Звіт ЛР1–ЛР4 (" + inputPath + ") ===");
        try {
            List<String> lines = Files.readAllLines(inputPath, StandardCharsets.UTF_8);
            System.out.println(processLines(lines));
        } catch (IOException exception) {
            System.err.println("Помилка читання файлу: " + exception.getMessage());
        }

        System.out.println("=== ЛР5: Repository<VetVisitRecord> і CSV round-trip (" + recordsPath + ") ===");
        try {
            Repository<VetVisitRecord> repository = loadRepository(recordsPath);
            System.out.print(exportAndVerify(repository, outputDir));
            System.out.printf(Locale.ROOT, "Візитів дорожче 1000: %d%n",
                    repository.find(record -> record.cost() > 1000.0).size());
        } catch (DataStorageException exception) {
            printError(exception);
        }

        if (args.length <= 1 && Files.exists(DEFAULT_BAD_RECORDS)) {
            System.out.println();
            System.out.println("=== Обробка хибного CSV (" + DEFAULT_BAD_RECORDS + ") ===");
            try {
                loadRepository(DEFAULT_BAD_RECORDS);
            } catch (DataStorageException exception) {
                System.out.println("Помилка: " + exception.getMessage());
                System.out.println("Причина: " + exception.getCause());
            }
        }
    }

    /* Друкує помилку зберігання разом із першопричиною. */
    private static void printError(DataStorageException exception) {
        System.err.println("Помилка зберігання даних: " + exception.getMessage());
        if (exception.getCause() != null) {
            System.err.println("Причина: " + exception.getCause());
        }
    }
}
