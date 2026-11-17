package ua.lpnu.kzp;

import java.util.ArrayList;
import java.util.List;

/**
 * Документний CSV-парсер на основі машини станів.
 *
 * <p>Розрізняє роздільник і перенесення рядка всередині та поза лапками,
 * тому коректно читає поля з комами, подвоєними лапками та multiline-значеннями.
 * Повертає список записів, а не фізичних рядків файлу.</p>
 */
public final class CsvParser {

    /** Типовий роздільник полів CSV. */
    public static final char COMMA = ',';

    private CsvParser() {
        // Забороняє створення екземплярів статичного парсера
    }

    /**
     * Розбирає один CSV-запис із роздільником кома.
     *
     * @param line текст одного запису
     * @return незмінний список полів
     * @throws DataStorageException якщо текст містить не один запис або має хибні лапки
     */
    public static List<String> parse(String line) throws DataStorageException {
        return parse(line, COMMA);
    }

    /**
     * Розбирає один CSV-запис із заданим роздільником.
     *
     * @param line      текст одного запису
     * @param delimiter роздільник полів, наприклад {@code ','} або {@code ';'}
     * @return незмінний список полів
     * @throws DataStorageException якщо текст містить не один запис або має хибні лапки
     */
    public static List<String> parse(String line, char delimiter) throws DataStorageException {
        List<List<String>> records = parseDocument(line, delimiter);
        if (records.size() != 1) {
            throw new DataStorageException(
                    "Очікується один CSV-рядок, знайдено " + records.size());
        }
        return records.get(0);
    }

    /**
     * Розбирає CSV-документ із роздільником кома.
     *
     * @param text повний текст документа
     * @return незмінний список записів; порожній текст дає порожній список
     * @throws DataStorageException якщо текст дорівнює null або лапки не закриті
     */
    public static List<List<String>> parseDocument(String text) throws DataStorageException {
        return parseDocument(text, COMMA);
    }

    /**
     * Розбирає CSV-документ із заданим роздільником.
     *
     * @param text      повний текст документа
     * @param delimiter роздільник полів
     * @return незмінний список записів; порожній текст дає порожній список
     * @throws DataStorageException якщо текст дорівнює null, роздільник недопустимий
     *                              або лапки не закриті
     */
    public static List<List<String>> parseDocument(String text, char delimiter)
            throws DataStorageException {
        if (text == null) {
            throw new DataStorageException("CSV-документ не може бути null");
        }
        if (delimiter == '"' || delimiter == '\r' || delimiter == '\n') {
            throw new DataStorageException("Недопустимий роздільник CSV");
        }
        if (text.isEmpty()) {
            return List.of();
        }
        List<List<String>> records = new ArrayList<>();
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        // quoted показує, що поточне поле перебуває всередині лапок, а quoteClosed —
        // що лапки вже закриті й далі допустимий лише роздільник або кінець запису.
        boolean quoted = false;
        boolean quoteClosed = false;
        for (int index = 0; index < text.length(); index++) {
            char symbol = text.charAt(index);
            if (symbol == '"' && !quoteClosed && !quoted && current.length() == 0) {
                quoted = true;
            } else if (symbol == '"' && quoted
                    && index + 1 < text.length() && text.charAt(index + 1) == '"') {
                // Подвоєна лапка всередині поля означає одну лапку в даних
                current.append('"');
                index++;
            } else if (symbol == '"' && quoted) {
                quoted = false;
                quoteClosed = true;
            } else if (symbol == delimiter && !quoted) {
                fields.add(current.toString());
                current.setLength(0);
                quoteClosed = false;
            } else if ((symbol == '\r' || symbol == '\n') && !quoted) {
                fields.add(current.toString());
                records.add(List.copyOf(fields));
                fields = new ArrayList<>();
                current.setLength(0);
                quoteClosed = false;
                // \r\n (Windows) вважається одним роздільником записів
                if (symbol == '\r' && index + 1 < text.length()
                        && text.charAt(index + 1) == '\n') {
                    index++;
                }
            } else {
                if (quoteClosed) {
                    throw new DataStorageException(
                            "Символ після закритих CSV-лапок у позиції " + index);
                }
                current.append(symbol);
            }
        }
        if (quoted) {
            throw new DataStorageException("Незакриті CSV-лапки");
        }
        if (!fields.isEmpty() || current.length() > 0) {
            fields.add(current.toString());
            records.add(List.copyOf(fields));
        }
        return List.copyOf(records);
    }
}
