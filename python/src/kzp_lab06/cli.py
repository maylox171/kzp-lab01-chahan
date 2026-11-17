"""Командний рядок: kzp-lab06 <input.csv> <report.txt> [--threshold N]."""

import argparse
import sys
from pathlib import Path

from .analysis import THRESHOLD, make_summary
from .errors import DataError
from .loader import load_records
from .reporting import format_report, write_report

EXIT_OK = 0
EXIT_DATA_ERROR = 2


def _use_utf8() -> None:
    """Перемикає stdout і stderr на UTF-8 (консоль Windows може мати інше кодування)."""
    for stream in (sys.stdout, sys.stderr):
        try:
            stream.reconfigure(encoding="utf-8")  # type: ignore[union-attr]
        except (AttributeError, ValueError):
            pass


def build_parser() -> argparse.ArgumentParser:
    """Створює парсер аргументів CLI."""
    parser = argparse.ArgumentParser(
        prog="kzp-lab06", description="Звіт ветклініки за CSV-файлом візитів."
    )
    parser.add_argument("input_csv", type=Path, help="вхідний CSV (UTF-8)")
    parser.add_argument("output_report", type=Path, help="шлях до текстового звіту")
    parser.add_argument(
        "--threshold",
        type=float,
        default=THRESHOLD,
        help=f"поріг відбору cost > threshold (типово {THRESHOLD})",
    )
    return parser


def main(argv: list[str] | None = None) -> int:
    """Обробляє аргументи CLI, будує та записує звіт; повертає код завершення."""
    _use_utf8()
    args = build_parser().parse_args(argv)
    try:
        records = load_records(args.input_csv)
        summary = make_summary(records, threshold=args.threshold)
        text = format_report(summary)
        write_report(args.output_report, text)
    except DataError as error:
        print(f"Помилка даних: {error}", file=sys.stderr)
        return EXIT_DATA_ERROR
    except OSError as error:
        print(f"Не вдалося записати звіт: {error}", file=sys.stderr)
        return EXIT_DATA_ERROR
    print(text, end="")
    print(f"Звіт записано: {args.output_report}")
    return EXIT_OK


if __name__ == "__main__":
    raise SystemExit(main())
