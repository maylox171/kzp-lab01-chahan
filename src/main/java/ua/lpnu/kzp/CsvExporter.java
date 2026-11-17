package ua.lpnu.kzp;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Універсальний рефлексивний експортер анотованих полів у CSV з кодуванням UTF-8.
 *
 * <p>Експортер не знає жодного предметного класу: він отримує клас записів і
 * список екземплярів, знаходить поля з {@link CsvColumn} у класі та всіх його
 * суперкласах, сортує їх за Java-іменем і записує заголовок та значення.</p>
 */
public final class CsvExporter {

    private CsvExporter() {
        // Забороняє створення екземплярів статичного експортера
    }

    /**
     * Записує анотовані поля записів у UTF-8 CSV.
     *
     * @param <T>     тип записів
     * @param path    шлях до CSV-файла
     * @param type    клас записів; потрібен, щоб побудувати заголовок навіть для порожнього списку
     * @param records записи для експорту
     * @throws DataStorageException якщо клас не має CSV-полів, поле неможливо прочитати
     *                              або файл неможливо записати
     */
    public static <T> void write(Path path, Class<T> type, List<? extends T> records)
            throws DataStorageException {
        Objects.requireNonNull(path, "Шлях не може бути null");
        String csv = toCsv(type, records);
        try {
            Path parent = path.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            // TRUNCATE_EXISTING гарантує, що старі рядки не залишаться після коротшого експорту
            Files.writeString(path, csv, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE);
        } catch (IOException exception) {
            throw new DataStorageException(
                    "Не вдалося записати CSV: %s".formatted(path), exception);
        }
    }

    /**
     * Формує CSV-текст без запису у файл.
     *
     * @param <T>     тип записів
     * @param type    клас записів
     * @param records записи для експорту
     * @return CSV-текст із заголовком і рядками, розділеними {@link System#lineSeparator()}
     * @throws DataStorageException якщо клас не має CSV-полів або поле неможливо прочитати
     */
    public static <T> String toCsv(Class<T> type, List<? extends T> records)
            throws DataStorageException {
        Objects.requireNonNull(type, "Тип не може бути null");
        Objects.requireNonNull(records, "Записи не можуть бути null");
        List<Field> fields = annotatedFields(type);

        // Заголовок і значення будуються з одного детерміновано відсортованого набору полів
        String header = fields.stream()
                .map(field -> escape(field.getAnnotation(CsvColumn.class).value()))
                .collect(Collectors.joining(","));
        StringBuilder csv = new StringBuilder(header).append(System.lineSeparator());
        for (T record : records) {
            if (record == null) {
                throw new DataStorageException("Список містить null-запис");
            }
            List<String> values = new ArrayList<>();
            for (Field field : fields) {
                try {
                    values.add(escape(field.get(record)));
                } catch (IllegalAccessException exception) {
                    throw new DataStorageException(
                            "Не вдалося прочитати поле %s".formatted(field.getName()), exception);
                }
            }
            csv.append(String.join(",", values)).append(System.lineSeparator());
        }
        return csv.toString();
    }

    /**
     * Повертає заголовки CSV-стовпців класу в порядку експорту.
     *
     * @param type клас записів
     * @return незмінний список назв стовпців
     * @throws DataStorageException якщо клас не має CSV-полів або доступ до поля заборонено
     */
    public static List<String> headers(Class<?> type) throws DataStorageException {
        Objects.requireNonNull(type, "Тип не може бути null");
        return annotatedFields(type).stream()
                .map(field -> field.getAnnotation(CsvColumn.class).value())
                .toList();
    }

    /**
     * Екранує значення за правилами CSV: поле з комою, лапкою або перенесенням
     * рядка береться в лапки, а лапки всередині подвоюються.
     *
     * @param value значення поля; {@code null} стає порожнім полем
     * @return екранований текст
     */
    public static String escape(Object value) {
        String text = Objects.toString(value, "");
        boolean quoted = text.contains(",")
                || text.contains("\"")
                || text.contains("\r")
                || text.contains("\n");
        return quoted ? "\"%s\"".formatted(text.replace("\"", "\"\"")) : text;
    }

    /* Повертає доступні анотовані поля класу та суперкласів у стабільному порядку. */
    private static List<Field> annotatedFields(Class<?> type) throws DataStorageException {
        List<Field> fields = new ArrayList<>();
        // Проходимо ланцюжок успадкування до Object, щоб врахувати поля базового класу
        for (Class<?> current = type; current != null && current != Object.class;
                current = current.getSuperclass()) {
            fields.addAll(Arrays.stream(current.getDeclaredFields())
                    .filter(field -> field.isAnnotationPresent(CsvColumn.class))
                    .toList());
        }
        if (fields.isEmpty()) {
            throw new DataStorageException(
                    "Клас %s не має CSV-полів".formatted(type.getName()));
        }
        // Порядок від JVM не гарантований, тому сортуємо за Java-іменем поля
        fields.sort(Comparator.comparing(Field::getName));
        for (Field field : fields) {
            if (!field.trySetAccessible()) {
                throw new DataStorageException(
                        "Немає доступу до поля %s".formatted(field.getName()));
            }
        }
        return List.copyOf(fields);
    }
}
