package ua.lpnu.kzp;

import java.util.Objects;

/**
 * Ургентний візит до ветклініки.
 */
public final class UrgentVisit extends VetVisit {

    public UrgentVisit(String animal, String species, String visitType, double price) {
        super(
            validateAnimal(animal),
            validateSpecies(species),
            validateVisitType(visitType),
            validatePrice(price),
            VisitKind.URGENT
        );
    }

    private static String validateAnimal(String animal) {
        String checked = Objects.requireNonNull(animal, "Кличка не може бути null");
        if (checked.isBlank()) {
            throw new IllegalArgumentException("Кличка не може бути порожньою");
        }
        return checked;
    }

    private static String validateSpecies(String species) {
        String checked = Objects.requireNonNull(species, "Вид не може бути null");
        if (checked.isBlank()) {
            throw new IllegalArgumentException("Вид не може бути порожнім");
        }
        return checked;
    }

    private static String validateVisitType(String visitType) {
        String checked = Objects.requireNonNull(visitType, "Тип візиту не може бути null");
        if (checked.isBlank()) {
            throw new IllegalArgumentException("Тип візиту не може бути порожнім");
        }
        return checked;
    }

    private static double validatePrice(double price) {
        if (price < 0 || !Double.isFinite(price)) {
            throw new IllegalArgumentException("ціна не може бути від'ємною");
        }
        return price;
    }

    @Override
    public double calculateFinalCost() {
        return getPrice() * 1.20;
    }
}