package ua.lpnu.kzp;

import java.time.LocalDate;

/**
 * Плановий візит до ветклініки: підсумкова вартість дорівнює базовій.
 */
public final class RoutineVisit extends VetVisit {

    /**
     * Створює плановий візит.
     *
     * @param animal    кличка тварини
     * @param species   вид тварини
     * @param owner     власник
     * @param date      дата візиту
     * @param diagnosis діагноз
     * @param visitType тип візиту
     * @param price     базова вартість
     * @throws IllegalArgumentException якщо значення порушують інваріанти моделі
     */
    public RoutineVisit(String animal, String species, String owner, LocalDate date,
            String diagnosis, String visitType, double price) {
        super(requireText(animal, "Кличка"), requireText(species, "Вид"), requireText(owner, "Власник"),
                requireDate(date), requireText(diagnosis, "Діагноз"), requireText(visitType, "Тип візиту"),
                requirePrice(price), VisitKind.ROUTINE);
    }

    /**
     * Повертає вартість планового візиту без надбавки.
     *
     * @return базова вартість
     */
    @Override
    public double calculateFinalCost() {
        return getPrice();
    }
}
