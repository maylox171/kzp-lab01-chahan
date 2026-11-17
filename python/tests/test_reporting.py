"""Тести формування й запису звіту."""

from pathlib import Path

from kzp_lab06.analysis import make_summary
from kzp_lab06.loader import load_records
from kzp_lab06.reporting import format_report, write_report

EXPECTED_REPORT = """Звіт ветклініки (варіант 21)
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
"""


def test_full_report_text(data_dir) -> None:
    """Звіт на контрольному CSV збігається з очікуваним текстом."""
    summary = make_summary(load_records(data_dir / "vet_visits.csv"))
    assert format_report(summary) == EXPECTED_REPORT


def test_report_for_empty_input() -> None:
    """Порожній вхід дає коректний звіт без ділення на нуль."""
    text = format_report(make_summary([]))
    assert "Кількість коректних записів: 0" in text
    assert "Середня вартість візиту: 0.00" in text
    assert "Найдорожчий візит: немає" in text
    assert "(немає записів)" in text


def test_write_report_creates_utf8_file(tmp_path: Path) -> None:
    """Звіт записується у вкладений каталог у UTF-8 з роздільником \\n."""
    output = tmp_path / "reports" / "summary.txt"
    write_report(output, "Готово\nЗвіт\n")
    assert output.read_bytes() == "Готово\nЗвіт\n".encode()
