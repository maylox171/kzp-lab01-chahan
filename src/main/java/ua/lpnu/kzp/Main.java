package ua.lpnu.kzp;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Main {

    /**
     * Обробляє список вхідних рядків CSV, створює об'єкти візитів,
     * збирає статистику та формує звіт для тестів і консолі.
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

    public static void main(String[] args) {
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8));

        Path inputPath = Path.of("data", "input.csv");
        if (!Files.exists(inputPath)) {
            System.err.println("Файл " + inputPath + " не знайдено!");
            return;
        }

        try {
            List<String> lines = Files.readAllLines(inputPath, StandardCharsets.UTF_8);
            String report = processLines(lines);
            System.out.println(report);
        } catch (Exception e) {
            System.err.println("Помилка читання файлу: " + e.getMessage());
        }
    }
}