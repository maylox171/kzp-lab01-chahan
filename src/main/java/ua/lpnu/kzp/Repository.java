package ua.lpnu.kzp;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Типобезпечне сховище об'єктів у пам'яті.
 *
 * <p>Параметр типу {@code T} фіксується під час створення, тому
 * {@code Repository<VetVisit>} приймає лише візити й не потребує приведень типів.</p>
 *
 * @param <T> тип записів, що зберігаються
 */
public final class Repository<T> {

    private final List<T> items = new ArrayList<>();

    /**
     * Створює порожнє сховище.
     */
    public Repository() {
        // Список ініціалізується під час оголошення поля
    }

    /**
     * Додає непорожній запис до сховища.
     *
     * @param item запис для додавання
     * @throws NullPointerException якщо {@code item} дорівнює {@code null}
     */
    public void add(T item) {
        items.add(Objects.requireNonNull(item, "Запис не може бути null"));
    }

    /**
     * Повертає незмінний знімок усіх записів у порядку додавання.
     *
     * @return незмінна копія внутрішнього списку
     */
    public List<T> all() {
        return List.copyOf(items);
    }

    /**
     * Повертає записи, що задовольняють умову.
     *
     * <p>{@code Predicate<? super T>} дозволяє передати умову для супертипу,
     * наприклад {@code Predicate<Object>}.</p>
     *
     * @param condition умова відбору
     * @return незмінний список знайдених записів
     */
    public List<T> find(Predicate<? super T> condition) {
        Objects.requireNonNull(condition, "Умова не може бути null");
        return items.stream()
                .filter(condition)
                .toList();
    }

    /**
     * Повертає кількість записів у сховищі.
     *
     * @return кількість записів
     */
    public int size() {
        return items.size();
    }
}
