package ua.lpnu.kzp;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MainTest {

    @Test
    void testValidRecords() {
        // Перевіряємо "щасливий шлях" (коректні дані)
        List<String> lines = List.of(
            "Барсік;Кіт;Огляд;450.50;false",
            "Рекс;Собака;Хірургія;2500.00;true"
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
            "Мурка;Кіт;Травма;-150.00;true"
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
        
        assertTrue(report.contains("очікується 5 полів, знайдено 3"));
        assertTrue(report.contains("Кількість коректних записів: 0"));
    }

    @Test
    void testNumberFormatException() {
        // Перевіряємо текст замість числа у полі ціни
        List<String> lines = List.of(
            "Рижик;Кіт;Огляд;дорого;false"
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
}