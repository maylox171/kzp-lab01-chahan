"""Спільні фікстури тестів."""

from pathlib import Path

import pytest

DATA_DIR = Path(__file__).resolve().parents[1] / "data"


@pytest.fixture
def data_dir() -> Path:
    """Повертає каталог із контрольними CSV-файлами."""
    return DATA_DIR


@pytest.fixture
def write_csv(tmp_path: Path):
    """Створює тимчасовий CSV-файл UTF-8 із заданим текстом."""

    def _write(text: str, name: str = "input.csv") -> Path:
        path = tmp_path / name
        path.write_text(text, encoding="utf-8", newline="")
        return path

    return _write
