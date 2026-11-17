package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class VetVisitTest {

    private static final LocalDate DAY = LocalDate.of(2026, 3, 1);

    @Test
    void subtypesHaveDifferentBehavior() throws DataStorageException {
        // Перевіряємо різну поліморфну поведінку підтипів (розрахунок вартості)
        VetVisit routine = new RoutineVisit("Барсік", "Кіт", "Іваненко Олена", DAY, "Огляд", "Огляд", 500.0);
        VetVisit urgent = new UrgentVisit("Рекс", "Собака", "Петренко Олег", DAY, "Перелом", "Хірургія", 1000.0);

        assertEquals(VisitKind.ROUTINE, routine.getVisitKind());
        assertEquals(VisitKind.URGENT, urgent.getVisitKind());

        // Поліморфний метод дає різні результати: ургентна надбавка 20 %
        assertNotEquals(routine.calculateFinalCost(), urgent.calculateFinalCost());
        assertEquals(1200.0, urgent.calculateFinalCost(), 1e-9);
    }

    @Test
    void equalObjectsHaveOneSetEntry() {
        // Логічно рівні об'єкти (усі поля однакові) дають один елемент множини
        Set<VetVisit> items = new HashSet<>();
        items.add(new RoutineVisit("Барсік", "Кіт", "Іваненко Олена", DAY, "Огляд", "Огляд", 500.0));
        items.add(new RoutineVisit("Барсік", "Кіт", "Іваненко Олена", DAY, "Огляд", "Огляд", 500.0));
        assertEquals(1, items.size());

        // Інша ціна — інший стан, отже інший об'єкт після round-trip
        items.add(new RoutineVisit("Барсік", "Кіт", "Іваненко Олена", DAY, "Огляд", "Огляд", 700.0));
        assertEquals(2, items.size());
    }

    @Test
    void subtypeRejectsInvalidPrice() {
        // Перевіряє відхилення неприпустимого специфічного значення (від'ємна ціна)
        assertThrows(IllegalArgumentException.class,
                () -> new RoutineVisit("Мурка", "Кіт", "Сидоренко Марія", DAY, "Травма", "Травма", -150.0));
    }

    @Test
    void modelRejectsBlankOwnerAndMissingDate() {
        assertThrows(IllegalArgumentException.class,
                () -> new RoutineVisit("Мурка", "Кіт", " ", DAY, "Травма", "Травма", 100.0));
        assertThrows(NullPointerException.class,
                () -> new UrgentVisit("Мурка", "Кіт", "Сидоренко Марія", null, "Травма", "Травма", 100.0));
    }

    @Test
    void testFromInputLineRoutine() throws DataStorageException {
        VetVisit visit = VetVisit.fromInputLine("Тузік;Собака;Мельник Петро;2026-03-08;Огляд;Вакцинація;300.0;false");
        assertEquals(RoutineVisit.class, visit.getClass());
        assertEquals("Тузік", visit.getAnimal());
        assertEquals(LocalDate.of(2026, 3, 8), visit.getDate());
    }

    @Test
    void testFromInputLineUrgent() throws DataStorageException {
        VetVisit visit = VetVisit.fromInputLine("Джек;Собака;Коваль Андрій;2026-03-04;Отруєння;Операція;2000.0;true");
        assertEquals(UrgentVisit.class, visit.getClass());
        assertEquals(VisitKind.URGENT, visit.getVisitKind());
    }

    @Test
    void fromInputLineSupportsQuotedFields() throws DataStorageException {
        VetVisit visit = VetVisit.fromInputLine(
                "Рекс;Собака;\"Петренко, Олег\";2026-03-02;\"Перелом лапи, \"\"складний\"\"\";Хірургія;2500.00;true");
        assertEquals("Петренко, Олег", visit.getOwner());
        assertEquals("Перелом лапи, \"складний\"", visit.getDiagnosis());
        assertEquals("Петренко, Олег|2026-03-02", visit.key());
    }
}
