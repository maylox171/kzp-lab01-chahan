package ua.lpnu.kzp;

/**
 * Категорія візиту до ветклініки.
 */
public enum VisitKind {
    ROUTINE("плановий"),
    URGENT("ургентний");

    private final String label;

    VisitKind(String label) {
        this.label = label;
    }

    /**
     * Повертає текстове подання категорії для звіту.
     */
    public String label() {
        return label;
    }
}