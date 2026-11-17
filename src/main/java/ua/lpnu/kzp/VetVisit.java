package ua.lpnu.kzp;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Запечатаний абстрактний базовий клас для записів про візити до ветклініки.
 *
 * <p>Поля, позначені {@link CsvColumn}, експортуються універсальним
 * {@link CsvExporter}. Порядок стовпців визначається сортуванням Java-імен полів:
 * {@code animal, date, diagnosis, owner, price, species, visitKind, visitType}.</p>
 *
 * <p>Інваріанти: текстові поля непорожні, дата задана, ціна скінченна й невід'ємна.
 * Ключ round-trip у звіті — {@code owner + date}.</p>
 */
public abstract sealed class VetVisit permits RoutineVisit, UrgentVisit {

    /** Кількість полів у рядку вхідного файлу та в CSV-експорті. */
    public static final int FIELD_COUNT = 8;

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

    @CsvColumn("тип візиту")
    private final String visitType;

    @CsvColumn("вартість")
    private final double price;

    @CsvColumn("категорія")
    private final VisitKind visitKind;

    /**
     * Створює запис візиту; викликається лише підтипами.
     *
     * <p>Значення вже перевірені статичними методами {@link #requireText},
     * {@link #requireDate} і {@link #requirePrice} в аргументах {@code super(...)}
     * фінальних підтипів, тому конструктор базового класу не кидає винятків
     * і об'єкт не залишається частково ініціалізованим.</p>
     *
     * @param animal    кличка тварини
     * @param species   вид тварини
     * @param owner     власник тварини
     * @param date      дата візиту
     * @param diagnosis діагноз
     * @param visitType тип візиту
     * @param price     базова вартість візиту
     * @param visitKind категорія візиту
     */
    protected VetVisit(String animal, String species, String owner, LocalDate date,
            String diagnosis, String visitType, double price, VisitKind visitKind) {
        this.animal = animal;
        this.species = species;
        this.owner = owner;
        this.date = date;
        this.diagnosis = diagnosis;
        this.visitType = visitType;
        this.price = price;
        this.visitKind = visitKind;
    }

    /**
     * Перевіряє, що текстове поле задане й непорожнє.
     *
     * @param value значення поля
     * @param name  назва поля для повідомлення
     * @return перевірене значення
     * @throws IllegalArgumentException якщо значення порожнє
     */
    protected static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " не може бути порожнім");
        }
        return value;
    }

    /**
     * Перевіряє, що дата візиту задана.
     *
     * @param date дата
     * @return перевірена дата
     * @throws NullPointerException якщо дата дорівнює {@code null}
     */
    protected static LocalDate requireDate(LocalDate date) {
        return Objects.requireNonNull(date, "Дата не може бути null");
    }

    /**
     * Перевіряє, що ціна скінченна й невід'ємна.
     *
     * @param price ціна
     * @return перевірена ціна
     * @throws IllegalArgumentException якщо ціна від'ємна або нескінченна
     */
    protected static double requirePrice(double price) {
        if (price < 0 || !Double.isFinite(price)) {
            throw new IllegalArgumentException("ціна не може бути від'ємною");
        }
        return price;
    }

    /**
     * Створює візит потрібного підтипу за категорією.
     *
     * @param animal    кличка тварини
     * @param species   вид тварини
     * @param owner     власник
     * @param date      дата візиту
     * @param diagnosis діагноз
     * @param visitType тип візиту
     * @param price     базова вартість
     * @param kind      категорія, що визначає підтип
     * @return {@link RoutineVisit} або {@link UrgentVisit}
     */
    public static VetVisit of(String animal, String species, String owner, LocalDate date,
            String diagnosis, String visitType, double price, VisitKind kind) {
        Objects.requireNonNull(kind, "Категорія не може бути null");
        return switch (kind) {
            case ROUTINE -> new RoutineVisit(animal, species, owner, date, diagnosis, visitType, price);
            case URGENT -> new UrgentVisit(animal, species, owner, date, diagnosis, visitType, price);
        };
    }

    /**
     * Розбирає рядок вхідного файлу {@code data/input.csv} (роздільник {@code ;}).
     *
     * <p>Порядок полів: {@code animal;species;owner;date;diagnosis;visitType;price;urgent}.
     * Поля можуть бути взяті в лапки, якщо містять роздільник або лапки.</p>
     *
     * @param line рядок вхідного файлу
     * @return відповідний підтип візиту
     * @throws DataStorageException якщо рядок має хибний формат або значення
     */
    public static VetVisit fromInputLine(String line) throws DataStorageException {
        Objects.requireNonNull(line, "Рядок не може бути null");
        List<String> fields = CsvParser.parse(line, ';');
        checkCount(fields);
        String urgentText = fields.get(7).trim();
        VisitKind kind = switch (urgentText.toLowerCase(Locale.ROOT)) {
            case "true" -> VisitKind.URGENT;
            case "false" -> VisitKind.ROUTINE;
            default -> throw new DataStorageException(
                    "поле urgent має бути true або false, знайдено: " + urgentText);
        };
        return build(fields.get(0), fields.get(1), fields.get(2), fields.get(3),
                fields.get(4), fields.get(5), fields.get(6), kind);
    }

    /**
     * Розбирає один CSV-рядок експорту (роздільник кома) у порядку стовпців експортера.
     *
     * @param line один CSV-запис
     * @return відповідний підтип візиту
     * @throws DataStorageException якщо рядок має хибний формат або значення
     */
    public static VetVisit fromCsv(String line) throws DataStorageException {
        return fromFields(CsvParser.parse(line));
    }

    /**
     * Створює візит із уже розібраних полів CSV-експорту.
     *
     * <p>Порядок полів: тварина, дата, діагноз, власник, вартість, вид, категорія, тип візиту.</p>
     *
     * @param fields поля одного CSV-запису
     * @return відповідний підтип візиту
     * @throws DataStorageException якщо кількість полів, число, дата, enum або значення хибні
     */
    public static VetVisit fromFields(List<String> fields) throws DataStorageException {
        Objects.requireNonNull(fields, "Поля не можуть бути null");
        checkCount(fields);
        VisitKind kind;
        try {
            kind = VisitKind.valueOf(fields.get(6).trim());
        } catch (IllegalArgumentException exception) {
            throw new DataStorageException(
                    "невідома категорія візиту: " + fields.get(6), exception);
        }
        return build(fields.get(0), fields.get(5), fields.get(3), fields.get(1),
                fields.get(2), fields.get(7), fields.get(4), kind);
    }

    /* Перевіряє кількість полів запису. */
    private static void checkCount(List<String> fields) throws DataStorageException {
        if (fields.size() != FIELD_COUNT) {
            throw new DataStorageException(
                    "очікується %d полів, знайдено %d".formatted(FIELD_COUNT, fields.size()));
        }
    }

    /* Перетворює текстові значення на типи моделі й загортає помилки з причиною. */
    private static VetVisit build(String animal, String species, String owner, String dateText,
            String diagnosis, String visitType, String priceText, VisitKind kind)
            throws DataStorageException {
        double price;
        try {
            price = Double.parseDouble(priceText.trim());
        } catch (NumberFormatException exception) {
            throw new DataStorageException("поле ціни має помилковий формат", exception);
        }
        LocalDate date;
        try {
            date = LocalDate.parse(dateText.trim());
        } catch (DateTimeParseException exception) {
            throw new DataStorageException(
                    "поле дати має помилковий формат (очікується YYYY-MM-DD)", exception);
        }
        try {
            return of(animal.trim(), species.trim(), owner.trim(), date,
                    diagnosis.trim(), visitType.trim(), price, kind);
        } catch (IllegalArgumentException exception) {
            // Помилка інваріанту моделі передається далі з першопричиною
            throw new DataStorageException(exception.getMessage(), exception);
        }
    }

    /**
     * Повертає кличку тварини.
     *
     * @return кличка
     */
    public final String getAnimal() {
        return animal;
    }

    /**
     * Повертає вид тварини.
     *
     * @return вид
     */
    public final String getSpecies() {
        return species;
    }

    /**
     * Повертає власника тварини.
     *
     * @return власник
     */
    public final String getOwner() {
        return owner;
    }

    /**
     * Повертає дату візиту.
     *
     * @return дата
     */
    public final LocalDate getDate() {
        return date;
    }

    /**
     * Повертає діагноз.
     *
     * @return діагноз
     */
    public final String getDiagnosis() {
        return diagnosis;
    }

    /**
     * Повертає тип візиту.
     *
     * @return тип візиту
     */
    public final String getVisitType() {
        return visitType;
    }

    /**
     * Повертає базову вартість візиту.
     *
     * @return вартість
     */
    public final double getPrice() {
        return price;
    }

    /**
     * Повертає категорію візиту.
     *
     * @return категорія
     */
    public final VisitKind getVisitKind() {
        return visitKind;
    }

    /**
     * Перевіряє, чи візит ургентний.
     *
     * @return {@code true} для ургентного візиту
     */
    public final boolean isUrgent() {
        return visitKind == VisitKind.URGENT;
    }

    /**
     * Повертає ключ round-trip: власник і дата.
     *
     * @return текстовий ключ {@code власник|дата}
     */
    public final String key() {
        return owner + "|" + date;
    }

    /**
     * Поліморфний метод для розрахунку підсумкової вартості візиту.
     *
     * @return вартість з урахуванням надбавок підтипу
     */
    public abstract double calculateFinalCost();

    /**
     * Порівнює візити одного підтипу за всіма полями.
     *
     * @param other інший об'єкт
     * @return {@code true}, якщо підтип і всі поля збігаються
     */
    @Override
    public final boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        VetVisit visit = (VetVisit) other;
        return Double.compare(price, visit.price) == 0
                && animal.equals(visit.animal)
                && species.equals(visit.species)
                && owner.equals(visit.owner)
                && date.equals(visit.date)
                && diagnosis.equals(visit.diagnosis)
                && visitType.equals(visit.visitType)
                && visitKind == visit.visitKind;
    }

    /**
     * Повертає хеш-код, узгоджений з {@link #equals(Object)}.
     *
     * @return хеш-код
     */
    @Override
    public final int hashCode() {
        return Objects.hash(getClass(), animal, species, owner, date, diagnosis, visitType,
                price, visitKind);
    }

    /**
     * Повертає подання візиту у форматі вхідного файлу.
     *
     * @return рядок із полями, розділеними {@code ;}
     */
    @Override
    public String toString() {
        return String.format(Locale.ROOT, "%s;%s;%s;%s;%s;%s;%.2f;%b",
                animal, species, owner, date, diagnosis, visitType, price, isUrgent());
    }
}
