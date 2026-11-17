package ua.lpnu.kzp;

/**
 * Повідомляє про помилку зберігання, формату CSV, значення поля або рефлексії.
 *
 * <p>Checked-виняток змушує код, що викликає експорт чи імпорт, явно
 * обробити помилку. Першопричина (наприклад, {@link java.io.IOException}
 * або {@link NumberFormatException}) зберігається та доступна через {@link #getCause()}.</p>
 */
public class DataStorageException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * Створює виняток із повідомленням.
     *
     * @param message зрозумілий опис операції, що не вдалася
     */
    public DataStorageException(String message) {
        super(message);
    }

    /**
     * Створює виняток із повідомленням і першопричиною.
     *
     * @param message зрозумілий опис операції, що не вдалася
     * @param cause   початкова технічна помилка
     */
    public DataStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
