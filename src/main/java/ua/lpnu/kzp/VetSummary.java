package ua.lpnu.kzp;

import java.util.Locale;

/**
 * Незмінний набір підсумкових показників для ветклініки.
 *
 * @param count        загальна кількість успішних візитів
 * @param totalPrice   загальний виторг
 * @param averagePrice середня вартість візиту
 * @param urgentCount  кількість ургентних візитів
 */
public record VetSummary(int count, double totalPrice, double averagePrice, int urgentCount) {
    public VetSummary {
        if (count < 0 || urgentCount < 0 || totalPrice < 0 || averagePrice < 0
                || !Double.isFinite(totalPrice) || !Double.isFinite(averagePrice)) {
            throw new IllegalArgumentException("Показники не можуть бути від'ємними або нескінченними");
        }
    }
}