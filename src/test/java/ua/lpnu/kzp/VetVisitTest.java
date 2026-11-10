package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class VetVisitTest {

    @Test
    void subtypesHaveDifferentBehavior() {
        // Перевіряємо різну поліморфну поведінку підтипів (розрахунок вартості)
        VetVisit routine = new RoutineVisit("Барсік", "Кіт", "Огляд", 500.0);
        VetVisit urgent = new UrgentVisit("Рекс", "Собака", "Хірургія", 1000.0);

        assertEquals(VisitKind.ROUTINE, routine.getVisitKind());
        assertEquals(VisitKind.URGENT, urgent.getVisitKind());
        
        // Поліморфний метод дає різні результати
        assertNotEquals(routine.calculateFinalCost(), urgent.calculateFinalCost());
    }

    @Test
    void equalObjectsHaveOneSetEntry() {
        // Перевіряє відсутність дублювання логічно рівних об'єктів у множині (equals/hashCode)
        Set<VetVisit> items = new HashSet<>();
        items.add(new RoutineVisit("Барсік", "Кіт", "Огляд", 500.0));
        items.add(new RoutineVisit("Барсік", "Кіт", "Огляд", 700.0)); // однаковий ключ за назвою/видом/типом

        assertEquals(1, items.size());
    }

    @Test
    void subtypeRejectsInvalidPrice() {
        // Перевіряє відхилення неприпустимого специфічного значення (від'ємна ціна)
        assertThrows(IllegalArgumentException.class,
                () -> new RoutineVisit("Мурка", "Кіт", "Травма", -150.0));
    }

    @Test
    void testFromCsvRoutine() {
        VetVisit visit = VetVisit.fromCsv("Тузік;Собака;Вакцинація;300.0;false");
        assertEquals(RoutineVisit.class, visit.getClass());
        assertEquals("Тузік", visit.getAnimal());
    }

    @Test
    void testFromCsvUrgent() {
        VetVisit visit = VetVisit.fromCsv("Джек;Собака;Операція;2000.0;true");
        assertEquals(UrgentVisit.class, visit.getClass());
        assertEquals(VisitKind.URGENT, visit.getVisitKind());
    }
}