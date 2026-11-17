package ua.lpnu.kzp;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Плоский запис візиту до ветклініки (варіант 21 лабораторної роботи № 5).
 *
 * <p>Поля та CSV-стовпці: animal — тварина, species — вид, owner — власник,
 * date — дата, diagnosis — діагноз, cost — вартість. Порядок стовпців у CSV
 * визначає {@link CsvExporter} сортуванням Java-імен полів:
 * {@code animal, cost, date, diagnosis, owner, species}.
 * Ключ round-trip — {@code owner + date}.</p>
 *
 * <p>Інваріанти: текстові поля непорожні, дата задана, вартість скінченна й невід'ємна.</p>
 */
public final class VetVisitRecord {

    /** Кількість CSV-полів запису. */
    public static final int FIELD_COUNT = 6;

    @CsvColumn("тварина")
    private final String animal;

    @CsvColumn("вид")
    private final String species;

    @CsvColumn("власник")
    private final String owner;

    @CsvColumn("дата")
    private final LocalDate date;

    @CsvColumn("діагноз")
    private final String diagnosis;

    @CsvColumn("вартість")
    private final double cost;

    /**
     * Створює перевірений запис візиту.
     *
     * @param animal    кличка тварини
     * @param species   вид тварини
     * @param owner     власник
     * @param date      дата візиту
     * @param diagnosis діагноз
     * @param cost      вартість візиту
     * @throws IllegalArgumentException якщо текстове поле порожнє, дата відсутня
     *                                  або вартість від'ємна чи нескінченна
     */
    public VetVisitRecord(String animal, String species, String owner, LocalDate date,
            String diagnosis, double cost) {
        if (isBlank(animal) || isBlank(species) || isBlank(owner) || isBlank(diagnosis)) {
            throw new IllegalArgumentException("Тварина, вид, власник і діагноз не можуть бути порожніми");
        }
        if (date == null) {
            throw new IllegalArgumentException("Дата не може бути порожньою");
        }
        if (cost < 0 || !Double.isFinite(cost)) {
            throw new IllegalArgumentException("вартість не може бути від'ємною");
        }
        this.animal = animal;
        this.species = species;
        this.owner = owner;
        this.date = date;
        this.diagnosis = diagnosis;
        this.cost = cost;
    }

    /* Перевіряє, що текст відсутній або складається лише з пробілів. */
    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /**
     * Розбирає один CSV-рядок у порядку стовпців експорту.
     *
     * @param line один CSV-запис
     * @return відновлений запис
     * @throws DataStorageException якщо рядок має хибний формат або значення
     */
    public static VetVisitRecord fromCsv(String line) throws DataStorageException {
        return fromFields(CsvParser.parse(line));
    }

    /**
     * Створює запис із розібраних CSV-полів у порядку
     * тварина, вартість, дата, діагноз, власник, вид.
     *
     * @param fields поля одного CSV-запису
     * @return відновлений запис
     * @throws DataStorageException якщо кількість полів, число, дата або значення хибні
     */
    public static VetVisitRecord fromFields(List<String> fields) throws DataStorageException {
        Objects.requireNonNull(fields, "Поля не можуть бути null");
        if (fields.size() != FIELD_COUNT) {
            throw new DataStorageException(
                    "очікується %d полів, знайдено %d".formatted(FIELD_COUNT, fields.size()));
        }
        double cost;
        try {
            cost = Double.parseDouble(fields.get(1).trim());
        } catch (NumberFormatException exception) {
            throw new DataStorageException("поле вартості має помилковий формат", exception);
        }
        LocalDate date;
        try {
            date = LocalDate.parse(fields.get(2).trim());
        } catch (DateTimeParseException exception) {
            throw new DataStorageException(
                    "поле дати має помилковий формат (очікується YYYY-MM-DD)", exception);
        }
        try {
            return new VetVisitRecord(fields.get(0), fields.get(5), fields.get(4), date,
                    fields.get(3), cost);
        } catch (IllegalArgumentException exception) {
            // Помилка інваріанту моделі передається далі з першопричиною
            throw new DataStorageException(exception.getMessage(), exception);
        }
    }

    /**
     * Читає CSV-файл експорту (UTF-8, перший запис — заголовок) у список записів.
     *
     * @param path шлях до CSV-файлу
     * @return записи в порядку файлу; порожній файл або лише заголовок дають порожній список
     * @throws DataStorageException якщо файл неможливо прочитати або запис хибний
     */
    public static List<VetVisitRecord> readCsv(Path path) throws DataStorageException {
        String csv;
        try {
            csv = Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new DataStorageException("Не вдалося прочитати CSV: %s".formatted(path), exception);
        }
        List<List<String>> rows = CsvParser.parseDocument(csv);
        List<VetVisitRecord> records = new ArrayList<>();
        for (int index = 1; index < rows.size(); index++) {
            try {
                records.add(fromFields(rows.get(index)));
            } catch (DataStorageException exception) {
                throw new DataStorageException(
                        "Запис %d файлу %s: %s".formatted(index + 1, path, exception.getMessage()),
                        exception);
            }
        }
        return List.copyOf(records);
    }

    /**
     * Повертає кличку тварини.
     *
     * @return кличка
     */
    public String animal() {
        return animal;
    }

    /**
     * Повертає вид тварини.
     *
     * @return вид
     */
    public String species() {
        return species;
    }

    /**
     * Повертає власника.
     *
     * @return власник
     */
    public String owner() {
        return owner;
    }

    /**
     * Повертає дату візиту.
     *
     * @return дата
     */
    public LocalDate date() {
        return date;
    }

    /**
     * Повертає діагноз.
     *
     * @return діагноз
     */
    public String diagnosis() {
        return diagnosis;
    }

    /**
     * Повертає вартість візиту.
     *
     * @return вартість
     */
    public double cost() {
        return cost;
    }

    /**
     * Повертає ключ round-trip: власник і дата.
     *
     * @return текстовий ключ {@code власник|дата}
     */
    public String key() {
        return owner + "|" + date;
    }

    /**
     * Порівнює записи за всіма полями.
     *
     * @param other інший об'єкт
     * @return {@code true}, якщо всі поля збігаються
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof VetVisitRecord record)) {
            return false;
        }
        return Double.compare(cost, record.cost) == 0
                && animal.equals(record.animal)
                && species.equals(record.species)
                && owner.equals(record.owner)
                && date.equals(record.date)
                && diagnosis.equals(record.diagnosis);
    }

    /**
     * Повертає хеш-код, узгоджений з {@link #equals(Object)}.
     *
     * @return хеш-код
     */
    @Override
    public int hashCode() {
        return Objects.hash(animal, species, owner, date, diagnosis, cost);
    }

    /**
     * Повертає коротке текстове подання запису.
     *
     * @return рядок з полями запису
     */
    @Override
    public String toString() {
        return String.format(Locale.ROOT, "%s (%s), %s, %s, %s: %.2f",
                animal, species, owner, date, diagnosis, cost);
    }
}
