# kzp-vet-report — Python-блок (ЛР6–ЛР7), варіант 21 «Ветклініка»

Студент: Чаган А. Ю. Пакет читає CSV, створений Java-застосунком ЛР5
(`data/vet_visits.csv`), і будує текстовий звіт ветклініки.

## Формат CSV

Заголовок (порядок стовпців не важливий, читання через `csv.DictReader`):

```text
animal,cost,date,diagnosis,owner,species,urgent,visit_type
```

Поля варіанта: `animal, species, owner, date, diagnosis, cost`; об'єднана модель додає
`visit_type` і `urgent`. Типи після читання: `cost` → `float` (≥ 0), `date` → `datetime.date`
(ISO `YYYY-MM-DD`), `urgent` → `bool` (`true`/`false`), текстові поля — `str` без пробілів по краях.

## Модулі `kzp_lab06`

| Модуль | Призначення |
|--------|-------------|
| `errors.py` | `DataError` — помилка даних із номером рядка |
| `loader.py` | читання CSV (`utf-8`, `newline=""`), перевірка заголовка, перетворення типів |
| `analysis.py` | відбір `cost > 1000.0`, кількість за видом, множина власників, показники, `make_summary` |
| `reporting.py` | детермінований текст звіту та запис через `with` |
| `cli.py` | `kzp-lab06 <input.csv> <report.txt> [--threshold N]`, коди 0 / 2 |

## Команди

```text
python -m venv .venv
.venv\Scripts\python -m pip install -e ".[dev]"     # Windows (.venv/bin/python у macOS/Ubuntu)
.venv\Scripts\python -m pytest
.venv\Scripts\ruff check .
.venv\Scripts\ruff format --check .
.venv\Scripts\python -m build
.venv\Scripts\kzp-lab06 data\vet_visits.csv reports\summary.txt
```
