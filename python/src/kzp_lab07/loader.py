"""Читання CSV ветклініки й перетворення рядків на об'єкти-візити."""

import csv
from datetime import date
from pathlib import Path

from .errors import DataError
from .models import RoutineVisit, UrgentVisit, VetVisit

REQUIRED_FIELDS = frozenset(
    {"animal", "species", "owner", "date", "diagnosis", "cost", "visit_type", "urgent"}
)
"""Обов'язкові стовпці CSV (ті самі, що в ЛР6)."""


def build_visit(row: dict[str, str | None], row_number: int) -> VetVisit:
    """Перетворює рядок DictReader на відповідний підтип візиту.

    Тип обирається один раз — тут, на межі завантаження; далі analysis працює
    через спільний протокол без перевірки конкретного класу.
    """
    missing = [name for name in sorted(REQUIRED_FIELDS) if row.get(name) is None]
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
    urgent = str(row["urgent"]).strip().lower()
    if urgent not in {"true", "false"}:
        raise DataError(f"Рядок {row_number}: поле urgent має бути true або false")
    visit_class = UrgentVisit if urgent == "true" else RoutineVisit
    try:
        return visit_class(
            animal=str(row["animal"]),
            species=str(row["species"]),
            owner=str(row["owner"]),
            visit_date=visit_date,
            diagnosis=str(row["diagnosis"]),
            cost=cost,
            visit_type=str(row["visit_type"]),
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
            missing = sorted(REQUIRED_FIELDS - set(reader.fieldnames))
            if missing:
                raise DataError(f"Відсутні стовпці CSV: {', '.join(missing)}")
            return [build_visit(row, reader.line_num) for row in reader]
    except OSError as error:
        raise DataError(f"Не вдалося прочитати файл: {path}") from error
    except UnicodeDecodeError as error:
        raise DataError(f"Файл {path} не є коректним UTF-8") from error
