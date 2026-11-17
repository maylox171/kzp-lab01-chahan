package ua.lpnu.kzp;

import java.util.Locale;
import java.util.Objects;

/**
 * Запечатаний абстрактний базовий клас для записів про візити до ветклініки.
 */
public abstract sealed class VetVisit permits RoutineVisit, UrgentVisit {
    private final String animal;
    private final String species;
    private final String visitType;
    private final double price;
    private final VisitKind visitKind;

    protected VetVisit(String animal, String species, String visitType, double price, VisitKind visitKind) {
        this.animal = animal;
        this.species = species;
        this.visitType = visitType;
        this.price = price;
        this.visitKind = visitKind;
    }

    /**
     * Фабричний метод для створення відповідного підтипу з CSV рядка.
     */
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
            boolean isUrgent = Boolean.parseBoolean(fields[4].trim());

            if (isUrgent) {
                return new UrgentVisit(animal, species, visitType, price);
            } else {
                return new RoutineVisit(animal, species, visitType, price);
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("поле ціни має помилковий формат", e);
        }
    }

    public final String getAnimal() { return animal; }
    public final String getSpecies() { return species; }
    public final String getVisitType() { return visitType; }
    public final double getPrice() { return price; }
    public final VisitKind getVisitKind() { return visitKind; }
    public final boolean isUrgent() { return visitKind == VisitKind.URGENT; }

    /**
     * Поліморфний метод для розрахунку підсумкової вартості візиту.
     */
    public abstract double calculateFinalCost();

    @Override
    public final boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        VetVisit visit = (VetVisit) other;
        return animal.equals(visit.animal) && species.equals(visit.species) && visitType.equals(visit.visitType);
    }

    @Override
    public final int hashCode() {
        return Objects.hash(getClass(), animal, species, visitType);
    }

    @Override
    public String toString() {
        return String.format(Locale.ROOT, "%s;%s;%s;%.2f;%b", animal, species, visitType, price, isUrgent());
    }
}