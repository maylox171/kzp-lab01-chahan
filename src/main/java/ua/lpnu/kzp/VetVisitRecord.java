package ua.lpnu.kzp;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Плоский запис візиту для машинного обміну з Python-блоком (лабораторні 6–7).
 *
 * <p>Має англомовні назви стовпців у {@link CsvColumn}, тому той самий
 * {@link CsvExporter} без змін формує файл {@code vet_visits.csv} з
 * заголовком {@code animal,cost,date,diagnosis,owner,species,urgent,visit_type}.
 * Це демонструє, що експортер працює з довільним класом.</p>
 */
public final class VetVisitRecord {

    @CsvColumn("animal")
    private final String animal;

    @CsvColumn("species")
    private final String species;

    @CsvColumn("owner")
    private final String owner;

    @CsvColumn("date")
    private final LocalDate date;

    @CsvColumn("diagnosis")
    private final String diagnosis;

    @CsvColumn("cost")
    private final double cost;

    @CsvColumn("visit_type")
    private final String visitType;

    @CsvColumn("urgent")
    private final boolean urgent;

    /**
     * Створює плаский запис із візиту предметної моделі.
     *
     * @param visit візит ветклініки
     */
    public VetVisitRecord(VetVisit visit) {
        Objects.requireNonNull(visit, "Візит не може бути null");
        this.animal = visit.getAnimal();
        this.species = visit.getSpecies();
        this.owner = visit.getOwner();
        this.date = visit.getDate();
        this.diagnosis = visit.getDiagnosis();
        this.cost = visit.getPrice();
        this.visitType = visit.getVisitType();
        this.urgent = visit.isUrgent();
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
     * Повертає вартість.
     *
     * @return вартість
     */
    public double cost() {
        return cost;
    }

    /**
     * Повертає тип візиту.
     *
     * @return тип візиту
     */
    public String visitType() {
        return visitType;
    }

    /**
     * Повертає ознаку ургентності.
     *
     * @return {@code true} для ургентного візиту
     */
    public boolean urgent() {
        return urgent;
    }
}
