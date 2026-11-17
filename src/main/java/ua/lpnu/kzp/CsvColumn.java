package ua.lpnu.kzp;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Позначає поле, яке експортується як CSV-стовпець.
 *
 * <p>Анотація доступна під час виконання ({@link RetentionPolicy#RUNTIME}),
 * тому {@link CsvExporter} читає її рефлексією. Застосовується лише до полів
 * ({@link ElementType#FIELD}). Анотація задає лише текст заголовка, а порядок
 * стовпців визначає експортер за Java-іменем поля.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface CsvColumn {
    /**
     * Повертає заголовок CSV-стовпця.
     *
     * @return назва стовпця у першому рядку CSV
     */
    String value();
}
