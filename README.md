# Ветклініка — наскрізний Java-продукт (КЗП, варіант 21)

Студент: Чаган А. Ю. Курс «Кросплатформні засоби програмування», НУ «Львівська політехніка».

Поточна версія: **1.5.0** (лабораторна робота № 5, гілка `lab05`, тег `v5.0.0`).

## Що робить програма

1. Читає `data/input.csv` (UTF-8, роздільник `;`) у форматі
   `animal;species;owner;date;diagnosis;visitType;price;urgent`.
2. Створює поліморфні об'єкти `RoutineVisit` / `UrgentVisit` і зберігає їх у `Repository<VetVisit>`.
3. Друкує звіт: кількість коректних записів, загальний виторг, середня вартість візиту,
   кількість ургентних візитів і список помилок із номерами рядків.
4. Експортує сховище рефлексивним `CsvExporter` у `target/data/vet_visits_export.csv`
   (українські заголовки з `@CsvColumn`) та `target/data/vet_visits.csv`
   (англомовні машинні назви для Python-блоку).
5. Читає експорт назад `CsvParser`-ом і перевіряє round-trip (`Round-trip: true`).

## Структура

| Файл | Призначення |
|------|-------------|
| `VetVisit`, `RoutineVisit`, `UrgentVisit`, `VisitKind` | модель візиту, підтипи з різною `calculateFinalCost()` (ургентний ×1.20) |
| `Repository<T>` | типобезпечне сховище: `add`, `all` (незмінний знімок), `find(Predicate<? super T>)` |
| `CsvColumn` | анотація поля з назвою CSV-стовпця (`RUNTIME`, `FIELD`) |
| `CsvExporter` | універсальний рефлексивний експортер будь-якого класу з анотованими полями |
| `CsvParser` | документний CSV-парсер на машині станів (лапки, коми, multiline, `\r\n`) |
| `DataStorageException` | власний checked-виняток із першопричиною (`getCause()`) |
| `VetVisitRecord` | плоский запис з англомовними стовпцями для обміну з Python |
| `VetReport`, `VetSummary` | потокові запити та підсумки з ЛР4 |
| `Main` | точка входу: звіт, експорт, round-trip |

## Формат CSV-експорту

Порядок стовпців — сортування Java-імен полів (`animal, date, diagnosis, owner, price, species, visitKind, visitType`):

```text
тварина,дата,діагноз,власник,вартість,вид,категорія,тип візиту
Рекс,2026-03-02,"Перелом лапи, ""складний""","Петренко, Олег",2500.0,Собака,URGENT,Хірургія
```

## Команди

```text
mvnw.cmd -B test          # Windows (./mvnw у macOS/Ubuntu)
mvnw.cmd -B verify        # тести + SpotBugs
mvnw.cmd -B package       # виконуваний JAR (Shade)
java -jar target/lab01-1.5.0.jar [вхідний.csv] [каталог-виводу]
```

CI (`.github/workflows/ci.yml`) виконує `verify`, `package`, запуск JAR на Ubuntu, Windows і macOS
та публікує JAR і згенеровані CSV як артефакти.
