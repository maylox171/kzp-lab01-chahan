package ua.lpnu.kzp;

import java.time.LocalDate;

/**
 * Ургентний візит до ветклініки: до базової вартості додається надбавка 20 %.
 */
public final class UrgentVisit extends VetVisit {

    /** Коефіцієнт ургентної надбавки. */
    public static final double URGENT_RATE = 1.20;

    /**
     * Створює ургентний візит.
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
    public UrgentVisit(String animal, String species, String owner, LocalDate date,
            String diagnosis, String visitType, double price) {
        super(requireText(animal, "Кличка"), requireText(species, "Вид"), requireText(owner, "Власник"),
                requireDate(date), requireText(diagnosis, "Діагноз"), requireText(visitType, "Тип візиту"),
                requirePrice(price), VisitKind.URGENT);
    }

    /**
     * Повертає вартість ургентного візиту з надбавкою.
     *
     * @return базова вартість, помножена на {@link #URGENT_RATE}
     */
    @Override
    public double calculateFinalCost() {
        return getPrice() * URGENT_RATE;
    }
}
