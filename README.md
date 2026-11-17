# Ветклініка — наскрізний Java-продукт (КЗП, варіант 21)

Студент: Чаган А. Ю. Курс «Кросплатформні засоби програмування», НУ «Львівська політехніка».

Поточна версія: **1.5.0** (лабораторна робота № 5, гілка `lab05`, тег `v5.0.0`).

## Що робить програма

1. **ЛР1–ЛР4 (без змін).** Читає `data/input.csv` (`animal;species;visitType;price;urgent`),
   створює `RoutineVisit` / `UrgentVisit` і друкує звіт: кількість коректних записів,
   загальний виторг, середня вартість, кількість ургентних візитів, помилки з номерами рядків.
2. **ЛР5.** Читає `data/vet_visits.csv` у `Repository<VetVisitRecord>` (варіант 21: поля
   `animal, species, owner, date, diagnosis, cost`), експортує сховище рефлексивним
   `CsvExporter` у `target/data/vet_visits_export.csv`, читає файл назад `CsvParser`-ом і
   перевіряє round-trip за ключем `owner + date`. Також показує обробку хибного файлу
   `data/vet_visits_bad.csv`.

## Структура

| Файл | Призначення |
|------|-------------|
| `VetVisit`, `RoutineVisit`, `UrgentVisit`, `VisitKind`, `VetReport`, `VetSummary` | модель і потокові запити ЛР1–ЛР4 |
| `VetVisitRecord` | запис варіанта 21 з анотованими полями, `fromCsv`, `fromFields`, `readCsv`, `key()` |
| `Repository<T>` | типобезпечне сховище: `add`, `all` (незмінний знімок), `find(Predicate<? super T>)` |
| `CsvColumn` | анотація поля з назвою CSV-стовпця (`RUNTIME`, `FIELD`) |
| `CsvExporter` | універсальний рефлексивний експортер будь-якого класу з анотованими полями |
| `CsvParser` | документний CSV-парсер на машині станів (лапки, коми, multiline, `\r\n`) |
| `DataStorageException` | власний checked-виняток із першопричиною (`getCause()`) |
| `Main` | точка входу: звіт ЛР1–ЛР4, експорт і round-trip ЛР5 |

## Формат CSV

Порядок стовпців — сортування Java-імен полів (`animal, cost, date, diagnosis, owner, species`):

```text
тварина,вартість,дата,діагноз,власник,вид
Рекс,2500.0,2026-03-02,"Перелом лапи, ""складний""","Петренко, Олег",Собака
```

Дані: `data/vet_visits.csv` (6 записів), `data/vet_visits_bad.csv` (хибна вартість),
`data/vet_visits_empty.csv` (порожній CSV).

## Команди

```text
mvnw.cmd -B test          # Windows (./mvnw у macOS/Ubuntu)
mvnw.cmd -B verify        # тести + SpotBugs
mvnw.cmd -B package       # виконуваний JAR (Shade)
java -jar target/lab01-1.5.0.jar [input.csv] [vet_visits.csv] [каталог-виводу]
```

CI (`.github/workflows/ci.yml`) виконує `verify`, `package`, запуск JAR на Ubuntu, Windows і macOS
та публікує JAR і згенерований CSV як артефакти.

## Python-блок (ЛР6–ЛР7)

Каталог `python/` містить пакети `kzp_lab06` (обробка CSV функціями, CLI `kzp-lab06`) і
`kzp_lab07` (об'єктна модель, CLI `kzp-lab07`), які читають `python/data/input.csv` з полями
варіанта 21 `animal,species,owner,date,diagnosis,cost`.
Опис — `python/README.md`, звіт — `python/REPORT.md`, CI — `.github/workflows/python.yml`.
