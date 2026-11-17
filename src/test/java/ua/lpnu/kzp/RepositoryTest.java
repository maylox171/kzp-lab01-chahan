package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;

class RepositoryTest {

    private static final LocalDate DAY = LocalDate.of(2026, 3, 1);

    /* Перевіряє типобезпечне сховище та пошук за умовою. */
    @Test
    void repositoryIsGenericAndFindsRecords() {
        Repository<VetVisit> repository = new Repository<>();
        repository.add(new RoutineVisit("Барсік", "Кіт", "Іваненко Олена", DAY, "Огляд", "Огляд", 450.5));
        repository.add(new UrgentVisit("Рекс", "Собака", "Петренко Олег", DAY, "Перелом", "Хірургія", 2500.0));

        assertEquals(2, repository.all().size());
        assertEquals(1, repository.find(VetVisit::isUrgent).size());
        assertEquals("Рекс", repository.find(v -> v.getPrice() > 1000).get(0).getAnimal());
    }

    /* Перевіряє, що all() повертає незмінний знімок. */
    @Test
    void allReturnsImmutableSnapshot() {
        Repository<String> repository = new Repository<>();
        repository.add("перший");
        List<String> snapshot = repository.all();
        repository.add("другий");

        assertEquals(1, snapshot.size());
        assertEquals(2, repository.size());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.add("третій"));
    }

    /* Перевіряє предикат для супертипу (? super T) і відхилення null. */
    @Test
    void findAcceptsSupertypePredicateAndRejectsNull() {
        Repository<VetVisit> repository = new Repository<>();
        repository.add(new RoutineVisit("Кеша", "Папуга", "Іваненко Олена", DAY, "Огляд", "Консультація", 200.0));
        Predicate<Object> any = item -> true;

        assertEquals(1, repository.find(any).size());
        assertTrue(repository.find(v -> false).isEmpty());
        assertThrows(NullPointerException.class, () -> repository.add(null));
        assertThrows(NullPointerException.class, () -> repository.find(null));
    }
}
