"""Об'єктно-орієнтована обробка даних ветклініки (лабораторна робота № 7, варіант 21)."""

from .analysis import THRESHOLD, best_by_final_cost, make_summary
from .errors import DataError
from .loader import load_visits
from .models import (
    URGENT_RATE,
    Costed,
    ReportSummary,
    RoutineVisit,
    SpeciesStat,
    UrgentVisit,
    VetVisit,
)
from .reporting import format_report, write_report

__all__ = [
    "THRESHOLD",
    "URGENT_RATE",
    "Costed",
    "DataError",
    "ReportSummary",
    "RoutineVisit",
    "SpeciesStat",
    "UrgentVisit",
    "VetVisit",
    "best_by_final_cost",
    "format_report",
    "load_visits",
    "make_summary",
    "write_report",
]

__version__ = "0.7.0"
