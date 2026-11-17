"""Читання CSV ветклініки й перетворення рядків на об'єкти-візити."""

import csv
from datetime import date
from pathlib import Path

from .errors import DataError
from .models import RoutineVisit, UrgentVisit, VetVisit

REQUIRED_FIELDS = ("animal", "species", "owner", "date", "diagnosis", "cost")
"""Поля CSV варіанта 21 (ті самі, що в ЛР6)."""

URGENT_DIAGNOSES = (
    "травма",
    "перелом",
    "отруєння",
    "кровотеча",
    "опік",
    "укус",
    "задуха",
    "непритомність",
)
"""Ключові слова ургентних діагнозів.

У CSV варіанта немає окремого стовпця ургентності, тому підтип обирається за діагнозом:
якщо діагноз (без урахування регістру) містить одне з цих слів — UrgentVisit,
інакше — RoutineVisit.
"""


def is_urgent_diagnosis(diagnosis: str) -> bool:
    """Перевіряє, чи діагноз належить до ургентних за URGENT_DIAGNOSES."""
    text = diagnosis.casefold()
    return any(keyword in text for keyword in URGENT_DIAGNOSES)


def build_visit(row: dict[str, str | None], row_number: int) -> VetVisit:
    """Перетворює рядок DictReader на відповідний підтип візиту.

    Тип обирається один раз — тут, на межі завантаження; далі analysis працює
    через спільний протокол без перевірки конкретного класу.
    """
    missing = [name for name in REQUIRED_FIELDS if row.get(name) is None]
    if missing:
        raise DataError(f"Рядок {row_number}: відсутнє значення поля {missing[0]}")
    if None in row:
        raise DataError(f"Рядок {row_number}: зайві значення поза заголовком")
    try:
        cost = float(str(row["cost"]))
    except ValueError as error:
        raise DataError(f"Рядок {row_number}: поле cost має бути числом") from error
    try:
        visit_date = date.fromisoformat(str(row["date"]).strip())
    except ValueError as error:
        raise DataError(f"Рядок {row_number}: поле date має формат YYYY-MM-DD") from error
    diagnosis = str(row["diagnosis"])
    visit_class = UrgentVisit if is_urgent_diagnosis(diagnosis) else RoutineVisit
    try:
        return visit_class(
            animal=str(row["animal"]),
            species=str(row["species"]),
            owner=str(row["owner"]),
            visit_date=visit_date,
            diagnosis=diagnosis,
            cost=cost,
        )
    except ValueError as error:
        # Помилку setter-а доповнюємо номером рядка й зберігаємо першопричину
        raise DataError(f"Рядок {row_number}: {error}") from error


def load_visits(path: Path) -> list[VetVisit]:
    """Читає CSV-файл (UTF-8) і повертає список об'єктів-візитів.

    Файл лише із заголовком дає порожній список; файл без заголовка — DataError.
    """
    try:
        with path.open("r", encoding="utf-8", newline="") as source:
            reader = csv.DictReader(source)
            if reader.fieldnames is None:
                raise DataError(f"Файл {path} порожній: немає заголовка CSV")
            missing = [name for name in REQUIRED_FIELDS if name not in reader.fieldnames]
            if missing:
                raise DataError(f"Відсутні стовпці CSV: {', '.join(missing)}")
            return [build_visit(row, reader.line_num) for row in reader]
    except OSError as error:
        raise DataError(f"Не вдалося прочитати файл: {path}") from error
    except UnicodeDecodeError as error:
        raise DataError(f"Файл {path} не є коректним UTF-8") from error
