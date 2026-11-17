"""Обробка CSV ветклініки засобами Python (лабораторна робота № 6, варіант 21)."""

from .analysis import THRESHOLD, make_summary
from .errors import DataError
from .loader import load_records
from .reporting import format_report, write_report

__all__ = [
    "THRESHOLD",
    "DataError",
    "format_report",
    "load_records",
    "make_summary",
    "write_report",
]

__version__ = "0.6.0"
