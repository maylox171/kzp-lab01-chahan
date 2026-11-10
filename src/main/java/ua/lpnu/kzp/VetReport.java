package ua.lpnu.kzp;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.DoubleSummaryStatistics;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Набір чистих потокових запитів до даних ветклініки.
 */
public final class VetReport {

    private VetReport() {
        // Забороняє створення екземплярів класу статичних запитів
    }

    /**
     * 1. Відбір візитів, вартість яких перевищує заданий поріг.
     */
    public static List<VetVisit> filterByPriceThreshold(List<VetVisit> visits, double threshold) {
        return streamOf(visits)
                .filter(v -> v.getPrice() > threshold)
                .toList();
    }

    /**
     * 2. Перетворення (мапінг) візитів у назви видів тварин (або типи візитів).
     */
    public static List<String> mapSpecies(List<VetVisit> visits) {
        return streamOf(visits)
                .map(VetVisit::getSpecies)
                .toList();
    }

    /**
     * 3. Групування кількості візитів за видом тварини з використанням EnumMap або HashMap.
     */
    public static Map<String, Long> countBySpecies(List<VetVisit> visits) {
        return streamOf(visits)
                .collect(Collectors.groupingBy(
                        VetVisit::getSpecies,
                        Collectors.counting()
                ));
    }

    /**
     * 4. Зведена статистика вартості візитів.
     */
    public static DoubleSummaryStatistics costStatistics(List<VetVisit> visits) {
        return streamOf(visits)
                .collect(Collectors.summarizingDouble(VetVisit::calculateFinalCost));
    }

    /**
     * 5. Вибір топ-N візитів за складеним компаратором: вартість за спаданням, потім кличка (або тип) за зростанням.
     */
    public static List<VetVisit> topVisitsByCost(List<VetVisit> visits, int limit) {
        if (limit < 0) {
            throw new IllegalArgumentException("Ліміт не може бути від'ємним");
        }
        Comparator<VetVisit> byCostAndAnimal = Comparator
                .comparingDouble(VetVisit::calculateFinalCost)
                .reversed()
                .thenComparing(VetVisit::getAnimal);

        return streamOf(visits)
                .sorted(byCostAndAnimal)
                .limit(limit)
                .toList();
    }

    /**
     * Optional-пошук візиту за назвою тварини (або власника / діагнозом).
     */
    public static Optional<VetVisit> findByAnimal(List<VetVisit> visits, String animal) {
        Objects.requireNonNull(animal, "Кличка не може бути null");
        return streamOf(visits)
                .filter(v -> v.getAnimal().equals(animal))
                .findFirst();
    }

    /**
     * Допоміжний метод для безпечного перетворення списку в потік.
     */
    private static Stream<VetVisit> streamOf(List<VetVisit> visits) {
        return Objects.requireNonNull(visits, "Список не може бути null").stream();
    }
}