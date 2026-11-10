package ua.lpnu.kzp;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VetVisitTest {

    @Test
    void validConstructorAndGetters() {
        VetVisit visit = new VetVisit("Барсик", "Кіт", "Огляд", 500.0, true);
        assertEquals("Барсик", visit.getAnimal());
        assertEquals("Кіт", visit.getSpecies());
        assertEquals("Огляд", visit.getVisitType());
        assertEquals(500.0, visit.getPrice(), 0.0001);
        assertTrue(visit.isUrgent());
    }

    @Test
    void constructorRejectsNegativePrice() {
        assertThrows(IllegalArgumentException.class,
                () -> new VetVisit("Барсик", "Кіт", "Огляд", -100.0, false));
    }

    @Test
    void constructorRejectsEmptyAnimal() {
        assertThrows(IllegalArgumentException.class,
                () -> new VetVisit("", "Кіт", "Огляд", 100.0, false));
    }

    @Test
    void fromCsvCreatesValidEntity() {
        VetVisit visit = VetVisit.fromCsv(" Рекс ; Собака ; Вакцинація ; 750.50 ; true ");
        assertEquals("Рекс", visit.getAnimal());
        assertEquals("Собака", visit.getSpecies());
        assertEquals("Вакцинація", visit.getVisitType());
        assertEquals(750.50, visit.getPrice(), 0.0001);
        assertTrue(visit.isUrgent());
    }

    @Test
    void fromCsvRejectsInvalidFieldsCount() {
        assertThrows(IllegalArgumentException.class,
                () -> VetVisit.fromCsv("Рекс;Собака;Вакцинація;750.50"));
    }

    @Test
    void fromCsvRejectsInvalidPriceNumber() {
        assertThrows(IllegalArgumentException.class,
                () -> VetVisit.fromCsv("Рекс;Собака;Вакцинація;abc;true"));
    }

    @Test
    void recordUsesValueEquality() {
        VetSummary first = new VetSummary(5, 2500.0, 500.0, 2);
        VetSummary second = new VetSummary(5, 2500.0, 500.0, 2);
        assertEquals(first, second);
    }
}