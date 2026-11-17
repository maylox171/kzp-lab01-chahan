"""Тести командного рядка та кодів завершення."""

import subprocess
import sys

from kzp_lab06.cli import EXIT_DATA_ERROR, EXIT_OK, main


def test_cli_success_writes_report(data_dir, tmp_path) -> None:
    """Успішний сценарій: код 0 і файл звіту."""
    output = tmp_path / "summary.txt"
    assert main([str(data_dir / "input.csv"), str(output)]) == EXIT_OK
    assert "Кількість візитів: 6" in output.read_text(encoding="utf-8")


def test_cli_data_error_returns_2(data_dir, tmp_path, capsys) -> None:
    """Пошкоджений CSV: код 2, повідомлення в stderr, звіт не створено."""
    output = tmp_path / "summary.txt"
    assert main([str(data_dir / "broken.csv"), str(output)]) == EXIT_DATA_ERROR
    assert "Помилка даних" in capsys.readouterr().err
    assert not output.exists()


def test_cli_threshold_option(data_dir, tmp_path) -> None:
    """Параметр --threshold змінює відбір (порожній результат для 5000)."""
    output = tmp_path / "summary.txt"
    assert main([str(data_dir / "input.csv"), str(output), "--threshold", "5000"]) == 0
    assert "(немає записів)" in output.read_text(encoding="utf-8")


def test_module_runs_as_script_without_side_effects_on_import(data_dir, tmp_path) -> None:
    """python -m kzp_lab06.cli працює, а імпорт модуля не запускає CLI."""
    output = tmp_path / "summary.txt"
    result = subprocess.run(
        [sys.executable, "-m", "kzp_lab06.cli", str(data_dir / "missing.csv"), str(output)],
        capture_output=True,
        check=False,
    )
    assert result.returncode == EXIT_DATA_ERROR
