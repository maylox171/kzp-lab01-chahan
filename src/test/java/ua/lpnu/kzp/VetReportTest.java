package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class VetReportTest {

    private static final LocalDate DAY = LocalDate.of(2026, 3, 1);

    private final List<VetVisit> visits = List.of(
            new RoutineVisit("Барсік", "Кіт", "Іваненко Олена", DAY, "Огляд", "Огляд", 500.0),
            new UrgentVisit("Рекс", "Собака", "Петренко Олег", DAY, "Перелом", "Операція", 2000.0),
            new RoutineVisit("Мурка", "Кіт", "Бондар Ірина", DAY, "Профілактика", "Вакцинація", 300.0),
            new UrgentVisit("Джек", "Собака", "Коваль Андрій", DAY, "Травма", "Травма", 1500.0),
            new RoutineVisit("Шарик", "Собака", "Мельник Петро", DAY, "Огляд", "Огляд", 400.0),
            new UrgentVisit("Пушок", "Кіт", "Ткачук Віра", DAY, "Отруєння", "Невідкладна допомога", 1200.0)
    );

    @Test
    void filtersByPriceThreshold() {
        List<VetVisit> filtered = VetReport.filterByPriceThreshold(visits, 600.0);
        assertEquals(3, filtered.size());
    }

    @Test
    void mapsSpeciesCorrectly() {
        List<String> species = VetReport.mapSpecies(visits);
        assertTrue(species.contains("Кіт"));
        assertTrue(species.contains("Собака"));
        assertEquals(6, species.size());
    }

    @Test
    void groupsAndCountsBySpecies() {
        Map<String, Long> counts = VetReport.countBySpecies(visits);
        assertEquals(3L, counts.get("Кіт"));
        assertEquals(3L, counts.get("Собака"));
    }

    @Test
    void calculatesCostStatistics() {
        DoubleSummaryStatistics stats = VetReport.costStatistics(visits);
        assertEquals(6, stats.getCount());
        assertTrue(stats.getSum() > 0);
    }

    @Test
    void ordersTopVisitsByCostAndAnimal() {
        List<VetVisit> top = VetReport.topVisitsByCost(visits, 3);
        assertEquals(3, top.size());
        // Найдорожчий візит повинен бути першим (Рекс, 2000 * 1.2 = 2400)
        assertEquals("Рекс", top.get(0).getAnimal());
    }

    @Test
    void findsVisitByAnimalUsingOptional() {
        Optional<VetVisit> found = VetReport.findByAnimal(visits, "Барсік");
        assertTrue(found.isPresent());
        assertEquals("Кіт", found.get().getSpecies());

        Optional<VetVisit> missing = VetReport.findByAnimal(visits, "Невідомий");
        assertFalse(missing.isPresent());
    }

    @Test
    void handlesEmptyInputAndNegativeLimit() {
        assertEquals(0, VetReport.filterByPriceThreshold(List.of(), 500.0).size());
        assertTrue(VetReport.findByAnimal(List.of(), "Барсік").isEmpty());
        assertThrows(IllegalArgumentException.class, () -> VetReport.topVisitsByCost(visits, -1));
    }

    @Test
    void queriesDoNotMutateInput() {
        List<VetVisit> before = List.copyOf(visits);
        VetReport.filterByPriceThreshold(visits, 500.0);
        VetReport.mapSpecies(visits);
        VetReport.countBySpecies(visits);
        VetReport.costStatistics(visits);
        VetReport.topVisitsByCost(visits, 3);
        assertEquals(before, visits);
    }
}
