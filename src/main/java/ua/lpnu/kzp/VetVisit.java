package ua.lpnu.kzp;

import java.util.Locale;
import java.util.Objects;

/**
 * Описує запис про візит до ветклініки.
 */
public final class VetVisit {
    private final String animal;
    private final String species;
    private final String visitType;
    private final double price;
    private final boolean urgent;

    public VetVisit(String animal, String species, String visitType, double price, boolean urgent) {
        this.animal = Objects.requireNonNull(animal, "Кличка не може бути null");
        if (animal.isBlank()) {
            throw new IllegalArgumentException("Кличка не може бути порожньою");
        }
        this.species = Objects.requireNonNull(species, "Вид не може бути null");
        if (species.isBlank()) {
            throw new IllegalArgumentException("Вид не може бути порожнім");
        }
        this.visitType = Objects.requireNonNull(visitType, "Тип візиту не може бути null");
        if (visitType.isBlank()) {
            throw new IllegalArgumentException("Тип візиту не може бути порожнім");
        }
        if (price < 0 || !Double.isFinite(price)) {
            throw new IllegalArgumentException("ціна не може бути від'ємною");
        }
        this.price = price;
        this.urgent = urgent;
    }

    public static VetVisit fromCsv(String line) {
        Objects.requireNonNull(line, "Рядок не може бути null");
        String[] fields = line.split(";", -1);
        if (fields.length != 5) {
            throw new IllegalArgumentException("очікується 5 полів, знайдено " + fields.length);
        }
        try {
            String animal = fields[0].trim();
            String species = fields[1].trim();
            String visitType = fields[2].trim();
            double price = Double.parseDouble(fields[3].trim());
            boolean booleanUrgent = Boolean.parseBoolean(fields[4].trim());

            return new VetVisit(animal, species, visitType, price, booleanUrgent);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("поле ціни має помилковий формат", e);
        }
    }

    public String getAnimal() { return animal; }
    public String getSpecies() { return species; }
    public String getVisitType() { return visitType; }
    public double getPrice() { return price; }
    public boolean isUrgent() { return urgent; }

    @Override
    public String toString() {
        return String.format(Locale.ROOT, "%s;%s;%s;%.2f;%b", animal, species, visitType, price, urgent);
    }
}