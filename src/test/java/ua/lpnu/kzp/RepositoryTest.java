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
        Repository<VetVisitRecord> repository = new Repository<>();
        repository.add(new VetVisitRecord("Барсік", "Кіт", "Іваненко Олена", DAY, "Огляд", 450.5));
        repository.add(new VetVisitRecord("Рекс", "Собака", "Петренко Олег", DAY, "Перелом", 2500.0));

        assertEquals(2, repository.all().size());
        assertEquals(1, repository.find(r -> r.cost() > 1000).size());
        assertEquals("Рекс", repository.find(r -> r.species().equals("Собака")).get(0).animal());
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
        Repository<VetVisitRecord> repository = new Repository<>();
        repository.add(new VetVisitRecord("Кеша", "Папуга", "Іваненко Олена", DAY, "Огляд", 200.0));
        Predicate<Object> any = item -> true;

        assertEquals(1, repository.find(any).size());
        assertTrue(repository.find(r -> false).isEmpty());
        assertThrows(NullPointerException.class, () -> repository.add(null));
        assertThrows(NullPointerException.class, () -> repository.find(null));
    }
}
