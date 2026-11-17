package ua.lpnu.kzp;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MainTest {

    @Test
    void testValidRecords() {
        // Перевіряємо "щасливий шлях" (коректні дані у форматі з власником, датою й діагнозом)
        List<String> lines = List.of(
            "Барсік;Кіт;Іваненко Олена;2026-03-01;Огляд;Огляд;450.50;false",
            "Рекс;Собака;Петренко Олег;2026-03-02;Перелом;Хірургія;2500.00;true"
        );
        String report = Main.processLines(lines);

        assertTrue(report.contains("Кількість коректних записів: 2"));
        assertTrue(report.contains("Загальний виторг: 2950.50"));
        assertTrue(report.contains("Помилок: 0"));
    }

    @Test
    void testNegativePrice() {
        // Перевіряємо, чи ловиться від'ємна ціна
        List<String> lines = List.of(
            "Мурка;Кіт;Сидоренко Марія;2026-03-07;Травма;Травма;-150.00;true"
        );
        String report = Main.processLines(lines);

        assertTrue(report.contains("Кількість коректних записів: 0"));
        assertTrue(report.contains("Помилок: 1"));
        assertTrue(report.contains("ціна не може бути від'ємною"));
    }

    @Test
    void testInvalidFieldCount() {
        // Перевіряємо рядок, де не вистачає полів
        List<String> lines = List.of(
            "Тузік;Собака;Огляд"
        );
        String report = Main.processLines(lines);

        assertTrue(report.contains("очікується 8 полів, знайдено 3"));
        assertTrue(report.contains("Кількість коректних записів: 0"));
    }

    @Test
    void testNumberFormatException() {
        // Перевіряємо текст замість числа у полі ціни
        List<String> lines = List.of(
            "Рижик;Кіт;Мельник Петро;2026-03-08;Огляд;Огляд;дорого;false"
        );
        String report = Main.processLines(lines);

        assertTrue(report.contains("поле ціни має помилковий формат"));
    }

    @Test
    void testEmptyList() {
        // Перевіряємо поведінку програми, якщо коректних записів немає взагалі
        List<String> lines = List.of();
        String report = Main.processLines(lines);

        assertTrue(report.contains("Кількість коректних записів: 0"));
        assertTrue(report.contains("Середня вартість візиту: 0.00")); // Щоб не було ділення на нуль
    }

    @Test
    void testUrgentCountAndAverage() {
        // Перевіряємо показники лабораторної роботи № 1: середню вартість і кількість ургентних
        List<String> lines = List.of(
            "Барсік;Кіт;Іваненко Олена;2026-03-01;Огляд;Огляд;100.00;false",
            "Рекс;Собака;\"Петренко, Олег\";2026-03-02;Перелом;Хірургія;300.00;true"
        );
        String report = Main.processLines(lines);

        assertTrue(report.contains("Середня вартість візиту: 200.00"));
        assertTrue(report.contains("Кількість ургентних візитів: 1"));
    }
}
