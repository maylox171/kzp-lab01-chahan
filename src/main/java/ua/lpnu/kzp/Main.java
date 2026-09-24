package ua.lpnu.kzp;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Головний клас програми для обробки даних ветеринарної клініки.
 */
public final class Main {
    
    private Main() {
    }

    /**
     * Точка входу до програми.
     * @param args аргументи командного рядка
     */
    public static void main(String[] args) {
        Path inputPath = Path.of("data", "input.csv");
        Path outputPath = Path.of("out", "report.txt");

        List<String> lines;
        try {
            lines = Files.readAllLines(inputPath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.out.println("Не вдалося прочитати файл: " + e.getMessage());
            return;
        }

        List<String> errors = new ArrayList<>();
        int validCount = 0;
        double totalRevenue = 0.0;
        int urgentCount = 0;

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            
            if (line.isBlank()) {
                continue;
            }

            String[] fields = line.split(";", -1);
            if (fields.length != 5) {
                errors.add(String.format("Рядок %d: очікується 5 полів, знайдено %d", i + 1, fields.length));
                continue;
            }

            if (fields[0].isBlank() || fields[1].isBlank() || fields[2].isBlank()) {
                errors.add(String.format("Рядок %d: текстові поля не можуть бути порожніми", i + 1));
                continue;
            }

            try {
                double price = Double.parseDouble(fields[3]);
                if (price < 0) {
                    errors.add(String.format("Рядок %d: ціна не може бути від'ємною", i + 1));
                    continue;
                }

                String urgentStr = fields[4].trim().toLowerCase();
                if (!urgentStr.equals("true") && !urgentStr.equals("false")) {
                    errors.add(String.format("Рядок %d: поле urgent має бути true або false", i + 1));
                    continue;
                }
                
                boolean isUrgent = Boolean.parseBoolean(urgentStr);

                // Якщо всі перевірки пройдені, додаємо до статистики
                validCount++;
                totalRevenue += price;
                if (isUrgent) {
                    urgentCount++;
                }

            } catch (NumberFormatException e) {
                errors.add(String.format("Рядок %d: поле ціни має помилковий формат", i + 1));
            }
        }

        double averagePrice = validCount == 0 ? 0.0 : totalRevenue / validCount;

        // Формування звіту
        StringBuilder report = new StringBuilder();
        report.append(String.format(Locale.ROOT, "Кількість коректних записів: %d%n", validCount));
        report.append(String.format(Locale.ROOT, "Загальний виторг: %.2f%n", totalRevenue));
        report.append(String.format(Locale.ROOT, "Середня вартість візиту: %.2f%n", averagePrice));
        report.append(String.format(Locale.ROOT, "Кількість ургентних візитів: %d%n", urgentCount));
        report.append(String.format(Locale.ROOT, "Помилок: %d%n", errors.size()));
        
        for (String error : errors) {
            report.append(error).append(System.lineSeparator());
        }

        // Вивід у консоль
        System.out.println(report.toString());

        // Запис у файл
        try {
            if (outputPath.getParent() != null) {
                Files.createDirectories(outputPath.getParent());
            }
            Files.writeString(outputPath, report.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.out.println("Помилка запису звіту у файл: " + e.getMessage());
        }
    }
}