"""Читання CSV ветклініки та перетворення рядків на типізовані записи."""

import csv
import math
from datetime import date
from pathlib import Path

from .errors import DataError

TEXT_FIELDS = ("animal", "species", "owner", "diagnosis", "visit_type")
"""Текстові поля, які нормалізуються (strip) і не можуть бути порожніми."""

REQUIRED_FIELDS = frozenset({*TEXT_FIELDS, "date", "cost", "urgent"})
"""Обов'язкові стовпці CSV (порядок стовпців у файлі не важливий)."""

Record = dict[str, object]


def parse_cost(value: str, row_number: int) -> float:
    """Перетворює вартість на невід'ємне скінченне число."""
    try:
        cost = float(value)
    except (TypeError, ValueError) as error:
        raise DataError(f"Рядок {row_number}: поле cost має бути числом") from error
    if not math.isfinite(cost) or cost < 0:
        raise DataError(f"Рядок {row_number}: cost не може бути від'ємним")
    return cost


def parse_date(value: str, row_number: int) -> date:
    """Перетворює дату ISO YYYY-MM-DD на об'єкт date."""
    try:
        return date.fromisoformat(value.strip())
    except (AttributeError, TypeError, ValueError) as error:
        raise DataError(f"Рядок {row_number}: поле date має формат YYYY-MM-DD") from error


def parse_urgent(value: str, row_number: int) -> bool:
    """Перетворює текст true/false на bool."""
    normalized = value.strip().lower()
    if normalized not in {"true", "false"}:
        raise DataError(f"Рядок {row_number}: поле urgent має бути true або false")
    return normalized == "true"


def parse_row(row: dict[str, str | None], row_number: int) -> Record:
    """Перевіряє один рядок DictReader і повертає типізований запис."""
    missing = [name for name in sorted(REQUIRED_FIELDS) if row.get(name) is None]
    if missing:
        raise DataError(f"Рядок {row_number}: відсутнє значення поля {missing[0]}")
    if None in row:
        raise DataError(f"Рядок {row_number}: зайві значення поза заголовком")
    record: Record = {}
    for name in TEXT_FIELDS:
        text = str(row[name]).strip()
        if not text:
            raise DataError(f"Рядок {row_number}: поле {name} порожнє")
        record[name] = text
    record["date"] = parse_date(str(row["date"]), row_number)
    record["cost"] = parse_cost(str(row["cost"]), row_number)
    record["urgent"] = parse_urgent(str(row["urgent"]), row_number)
    return record


def load_records(path: Path) -> list[Record]:
    """Читає CSV-файл (UTF-8) і повертає список типізованих записів.

    Правило порожнього входу: файл лише із заголовком дає порожній список,
    а повністю порожній файл (без заголовка) — DataError.
    """
    try:
        # newline="" передає обробку роздільників рядків модулю csv
        with path.open("r", encoding="utf-8", newline="") as source:
            reader = csv.DictReader(source)
            if reader.fieldnames is None:
                raise DataError(f"Файл {path} порожній: немає заголовка CSV")
            missing = sorted(REQUIRED_FIELDS - set(reader.fieldnames))
            if missing:
                raise DataError(f"Відсутні стовпці CSV: {', '.join(missing)}")
            records: list[Record] = []
            for row in reader:
                # line_num — номер фізичного рядка, на якому закінчився запис
                records.append(parse_row(row, reader.line_num))
            return records
    except OSError as error:
        raise DataError(f"Не вдалося прочитати файл: {path}") from error
    except UnicodeDecodeError as error:
        raise DataError(f"Файл {path} не є коректним UTF-8") from error
