# Звіт до лабораторної роботи № 7

(Звіт ЛР6 збережено у `python/REPORT_lab06.md`.)

## 1. Тема, номер і варіант

**Тема:** Об'єктно-орієнтована обробка даних у Python. Студент: Чаган А. Ю.
Варіант **21** — «Ветклініка»: базовий клас `VetVisit`, підтипи `RoutineVisit` і `UrgentVisit`,
спільна операція `final_cost()` з ургентною надбавкою, підсумок — візити за видами тварин.
ОС: Windows 11 (локально Python 3.15), CI — Ubuntu, Windows, macOS з Python 3.12.

## 2. Стан до і після

| ЛР6 (словники) | ЛР7 (об'єкти) |
|----------------|---------------|
| `dict[str, object]` на запис | екземпляр `RoutineVisit` / `UrgentVisit` |
| перевірки в `loader.parse_*` | перевірки в setter-ах `@property` |
| `make_summary` повертає `dict` | `ReportSummary` — `@dataclass(frozen=True)` |
| немає надбавки | `final_cost()`: плановий — `cost`, ургентний — `cost × 1.20` |
| `record["urgent"] is True` | атрибут підтипу `urgent`, тип обирається лише в loader |

Пакет `kzp_lab06` залишено без змін; новий пакет — `kzp_lab07`. CSV-поля та формат звіту ті самі.
Старий словниковий результат: виторг 5700.50, середня 950.08, ургентних 3, найдорожчий — Рекс 2500.00.
Новий об'єктний результат: ті самі значення + підсумкова вартість із надбавкою 6640.50.

## 3. Модель

`VetVisit.__init__(animal, species, owner, visit_date, diagnosis, cost, visit_type)` присвоює значення
через властивості, тому валідація спрацьовує й під час створення, й під час зміни.

| Властивість | Інваріант setter-а |
|-------------|--------------------|
| animal, species, owner, diagnosis, visit_type | непорожній рядок, пробіли по краях прибираються |
| date | лише `datetime.date` |
| cost | число (не bool), скінченне, ≥ 0; 0 і 1000.0 допустимі |
| key | лише читання: `(owner, date)` |

Setter записує внутрішнє поле (`self._cost`), а не властивість, — тому немає рекурсії.
Спеціальні методи: `__str__` — «Рекс (Собака), 2026-03-02, ургентний: 3000.00»; `__repr__` —
`RoutineVisit(animal='Барсік', ...)`; `__eq__` — однаковий підтип і повний стан, `NotImplemented`
для чужого типу; `__hash__ = None`, бо об'єкт змінюваний. Впорядкування — `sorted(..., key=order_key)`
(вартість за спаданням, потім кличка).

## 4. Dataclass

```python
@dataclass(frozen=True)
class SpeciesStat:
    species: str
    count: int
    total_final_cost: float


@dataclass(frozen=True)
class ReportSummary:
    count: int
    total_revenue: float
    average_cost: float
    urgent_count: int
    total_final_cost: float
    threshold: float
    most_expensive: VetVisit | None
    selected: tuple[VetVisit, ...]
    by_species: tuple[SpeciesStat, ...]
    owners: frozenset[str]
```

`frozen=True` забороняє змінювати підсумок після обчислення (`FrozenInstanceError`), а
`__eq__`/`__repr__` генеруються автоматично. Колекції — незмінні `tuple` і `frozenset`.

## 5. Підтипи та протокол

```python
class Costed(Protocol):
    @property
    def cost(self) -> float: ...

    def final_cost(self) -> float: ...
```

`RoutineVisit.final_cost()` повертає `cost`, `UrgentVisit.final_cost()` — `cost * URGENT_RATE`
(`URGENT_RATE = 1.20`, та сама формула, що в Java `UrgentVisit`). Функції `best_by_final_cost` і
`total_final_cost` приймають будь-які об'єкти з `final_cost()` (качина типізація): тест передає й
сторонній клас `Fee` без спільного предка. У `analysis` немає `isinstance` для підтипів —
тип обирається один раз у `loader.build_visit` за полем `urgent`.

## 6. Модулі

```text
python/src/kzp_lab07/
├── __init__.py    # публічний API
├── errors.py      # DataError
├── models.py      # VetVisit, RoutineVisit, UrgentVisit, Costed, SpeciesStat, ReportSummary
├── loader.py      # CSV → об'єкти, номер рядка, raise ... from
├── analysis.py    # відбір, підсумки, протокольні функції
├── reporting.py   # format_base_report (сумісний з ЛР6), format_report, write_report
└── cli.py         # kzp-lab07 <input.csv> <report.txt> [--threshold N], коди 0 / 2
```

## 7. Тестування

`python -m pytest` — **53 passed** (23 тести ЛР6 + 30 тестів ЛР7).

| Файл | Тестів | Що перевіряє |
|------|--------|--------------|
| test_lab07_models.py | 16 | setter: коректне, нульове, граничне 1000.0, 7 хибних значень (параметризовано); текст і дата; різні `final_cost()`; абстрактна операція; `__eq__`/`__repr__`/`__str__`/hash; дублікати; key-сортування; frozen dataclass; протокол з обома підтипами та стороннім класом |
| test_lab07_pipeline.py | 14 | loader обох підтипів, порожній вхід, 6 пошкоджених рядків з номером рядка, `__cause__` setter-а, відсутній стовпець і файл, підсумок за видами, граничне значення, порожній відбір, сумісність зі звітом ЛР6 (3 сценарії), CLI 0/2 |

## 8. Порівняння звітів

Тест `test_report_is_compatible_with_lab06` перевіряє, що `format_base_report` (ЛР7) символ у символ
дорівнює `format_report` ЛР6 на тому самому CSV, а повний звіт ЛР7 = звіт ЛР6 + новий розділ.
Також перевірено порожній вхід і поріг 300.0. Повний звіт ЛР7:

```text
Звіт ветклініки (варіант 21)
Кількість коректних записів: 6
Загальний виторг: 5700.50
Середня вартість візиту: 950.08
Кількість ургентних візитів: 3
Найдорожчий візит: Рекс (Собака, власник Петренко, Олег, 2026-03-02) — 2500.00
Візити з вартістю > 1000.00: 2
  - Рекс (Собака, власник Петренко, Олег, 2026-03-02) — 2500.00
  - Ласка (Тхір, власник Коваль Андрій, 2026-03-06) — 1200.00
Кількість візитів за видами:
  Кіт: 2
  Папуга: 1
  Собака: 2
  Тхір: 1
Власники (4): Іваненко Олена; Бондар Ірина; Коваль Андрій; Петренко, Олег

Підсумкова вартість з ургентною надбавкою (×1.20): 6640.50
Візити за видами тварин (кількість / підсумкова вартість):
  Кіт: 2 / 800.50
  Папуга: 1 / 200.00
  Собака: 2 / 4200.00
  Тхір: 1 / 1440.00
```

## 9. Інфраструктура

```text
python -m venv .venv
.venv\Scripts\python -m pip install -e ".[dev]"
.venv\Scripts\python -m pytest            → 53 passed
.venv\Scripts\ruff check .                → All checks passed!
.venv\Scripts\ruff format --check .       → 22 files already formatted
.venv\Scripts\python -m build             → kzp_vet_report-0.7.0-py3-none-any.whl
python -m venv .venv-wheel
.venv-wheel\Scripts\python -m pip install --no-deps dist\kzp_vet_report-0.7.0-py3-none-any.whl
.venv-wheel\Scripts\kzp-lab07 data\vet_visits.csv reports\summary-lab07.txt   → код 0
.venv-wheel\Scripts\kzp-lab07 data\broken.csv reports\broken-lab07.txt        → код 2
```

Версія пакета 0.7.0, скрипти `kzp-lab06` і `kzp-lab07`. Workflow `python.yml` на Ubuntu/Windows/macOS
виконує pytest, ruff, build, встановлення wheel у чисте середовище й перевірку обох CLI (коди 0 і 2),
артефакт — `kzp-vet-report-<os>-<N>`. Тег — `v7.0.0`.

## 10. GitHub Issues і Pull Request

| Issue | Зміна | Перевірка |
|-------|-------|-----------|
| #16 | `VetVisit` з `@property` | test_lab07_models (setter, eq, repr) |
| #17 | підтипи, `Costed` | test_subtypes…, test_protocol… |
| #18 | frozen dataclass | test_summary_dataclasses_are_frozen… |
| #19 | модулі kzp_lab07 | test_lab07_pipeline |
| #20 | тести | pytest — 53 passed |
| #21 | CI, wheel, документація | зелений CI на 3 ОС |

Pull Request `lab07 → lab06` містить `Closes #16 … #21` і порівняння звітів.

## 11. Академічна доброчесність

ШІ-асистент Claude Code (Anthropic) у ролях розробника, валідатора, DevOps і документатора.
Прийнято: окремий пакет `kzp_lab07` замість переписування `kzp_lab06` (зберігає старий результат
для порівняння), `__hash__ = None` для змінюваної сутності, формулу надбавки з Java-коду.
Відхилено: перевірки `isinstance` в analysis. Виправлено: затінення імені `date` властивістю класу
(імпорт `datetime as dt`). Власний внесок: вибір моделі, формул і перевірка пояснень кожного класу.

## 12. Відповіді на контрольні питання

1. Клас — опис структури й поведінки, екземпляр — конкретний об'єкт зі станом.
2. `__init__` встановлює початковий стан; `self` — посилання на поточний екземпляр.
3. Щоб читання/запис атрибута виконували перевірку й зберігали інваріант.
4. Інакше setter викликатиме сам себе рекурсивно.
5. `__init__`, `__repr__`, `__eq__` (і за потреби порівняння, hash).
6. Робить об'єкт незмінним, а намір — явним.
7. `__str__` — для читача, `__repr__` — для розробника/діагностики.
8. Повертати `NotImplemented`.
9. Коли критерій залежить від задачі аналізу, а не є сталою властивістю сутності.
10. Викликає наступну реалізацію в MRO (тут — ініціалізацію базового класу).
11. Порядок пошуку методів у ієрархії; важливий при множинному наслідуванні.
12. Важливо, що об'єкт уміє, а не його клас.
13. Формально описує потрібні методи для статичної перевірки без спільного предка.
14. Щоб новий підтип не вимагав змін у кожній функції.
15. У loader, на межі вводу.
16. Щоб імпорт у тестах не запускав програму.
17. Setter, незмінність dataclass, рівність, поліморфізм, протокол.
18. pytest — поведінка, ruff — якість, wheel — встановлюваність.
19. Порожній вхід, нуль і межа, хибні значення, дублікати, відсутні поля.
20. Класи, формули, звіти до/після, коди CLI, результати CI.

## 13. Висновки

Словникові записи замінено об'єктами з інкапсульованими властивостями, двома підтипами зі спільним
протоколом і незмінним підсумком. Зовнішній звіт ЛР6 збережено символ у символ, а новий розділ
показує поліморфну підсумкову вартість і візити за видами тварин. Модель готова до розширення
власним зберіганням або контекстними менеджерами.
