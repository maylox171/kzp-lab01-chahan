"""Тести loader, analysis, reporting і CLI ЛР7 та сумісності зі звітом ЛР6."""

import pytest

from kzp_lab06.analysis import make_summary as make_summary_v6
from kzp_lab06.loader import load_records
from kzp_lab06.reporting import format_report as format_report_v6
from kzp_lab07.analysis import make_summary
from kzp_lab07.cli import EXIT_DATA_ERROR, EXIT_OK, main
from kzp_lab07.errors import DataError
from kzp_lab07.loader import load_visits
from kzp_lab07.models import RoutineVisit, UrgentVisit
from kzp_lab07.reporting import format_base_report, format_report

HEADER = "animal,species,owner,date,diagnosis,cost,visit_type,urgent\n"

EXPECTED_EXTRA = """
Підсумкова вартість з ургентною надбавкою (×1.20): 6640.50
Візити за видами тварин (кількість / підсумкова вартість):
  Кіт: 2 / 800.50
  Папуга: 1 / 200.00
  Собака: 2 / 4200.00
  Тхір: 1 / 1440.00
"""


def test_loader_builds_both_subtypes(data_dir) -> None:
    """Loader створює об'єкти обох підтипів із контрольного CSV."""
    visits = load_visits(data_dir / "vet_visits.csv")

    assert len(visits) == 6
    assert sum(isinstance(visit, UrgentVisit) for visit in visits) == 3
    assert sum(isinstance(visit, RoutineVisit) for visit in visits) == 3
    assert visits[1].owner == "Петренко, Олег"
    assert visits[1].diagnosis == 'Перелом лапи, "складний"'


def test_empty_input(data_dir) -> None:
    """Лише заголовок — порожній список і звіт без ділення на нуль."""
    visits = load_visits(data_dir / "empty.csv")
    summary = make_summary(visits)

    assert visits == []
    assert summary.count == 0
    assert summary.average_cost == 0.0
    assert summary.most_expensive is None
    assert "Найдорожчий візит: немає" in format_report(summary)


@pytest.mark.parametrize(
    ("row", "message"),
    [
        ("Рижик,Кіт,Мельник Петро,2026-03-08,Огляд,дорого,Огляд,false", "cost має бути числом"),
        ("Мурка,Кіт,Сидоренко Марія,2026-03-07,Травма,-150,Травма,true", "від'ємним"),
        ("Бім,Собака,Лисенко Ганна,2026-13-40,Огляд,10,Огляд,false", "date"),
        ("Пушок,Кріль,Ткачук Віра,2026-03-09,Огляд,10,Огляд,так", "urgent"),
        ("Бім,Собака, ,2026-03-01,Огляд,10,Огляд,false", "owner"),
        ("Тузік,Собака,Огляд", "відсутнє значення"),
    ],
)
def test_broken_rows_have_row_number(write_csv, row: str, message: str) -> None:
    """Пошкоджений рядок дає DataError з номером рядка та причиною."""
    with pytest.raises(DataError, match=message) as info:
        load_visits(write_csv(HEADER + row + "\n"))
    assert "Рядок 2" in str(info.value)


def test_setter_error_keeps_cause(write_csv) -> None:
    """Помилка setter-а передається як __cause__."""
    with pytest.raises(DataError) as info:
        load_visits(write_csv(HEADER + "Мурка,Кіт,Сидоренко Марія,2026-03-07,Т,-1,Т,true\n"))
    assert isinstance(info.value.__cause__, ValueError)


def test_missing_column_and_missing_file(write_csv, tmp_path) -> None:
    """Відсутній стовпець і відсутній файл."""
    with pytest.raises(DataError, match="cost"):
        load_visits(write_csv("animal,species,owner,date,diagnosis,visit_type,urgent\n"))
    with pytest.raises(DataError) as info:
        load_visits(tmp_path / "missing.csv")
    assert isinstance(info.value.__cause__, OSError)


def test_summary_by_species_and_boundary(data_dir) -> None:
    """Підсумок за видами, граничне значення 1000.0 і дубльовані власники."""
    summary = make_summary(load_visits(data_dir / "vet_visits.csv"))

    assert [stat.species for stat in summary.by_species] == ["Кіт", "Папуга", "Собака", "Тхір"]
    assert summary.by_species[2].total_final_cost == pytest.approx(4200.0)
    assert [visit.animal for visit in summary.selected] == ["Рекс", "Ласка"]
    assert len(summary.owners) == 4
    assert summary.total_final_cost == pytest.approx(6640.5)
    assert make_summary(load_visits(data_dir / "vet_visits.csv"), threshold=5000.0).selected == ()


def test_report_is_compatible_with_lab06(data_dir) -> None:
    """Звіт ЛР7 починається точно зі звіту ЛР6 на тому самому CSV."""
    path = data_dir / "vet_visits.csv"
    old_text = format_report_v6(make_summary_v6(load_records(path)))
    summary = make_summary(load_visits(path))

    assert format_base_report(summary) == old_text
    assert format_report(summary) == old_text + EXPECTED_EXTRA


def test_report_compatible_for_empty_input_and_other_threshold(data_dir) -> None:
    """Сумісність зберігається для порожнього входу й іншого порогу."""
    for name, threshold in (("empty.csv", 1000.0), ("vet_visits.csv", 300.0)):
        path = data_dir / name
        old_text = format_report_v6(make_summary_v6(load_records(path), threshold=threshold))
        assert format_base_report(make_summary(load_visits(path), threshold)) == old_text


def test_cli_success_and_error(data_dir, tmp_path, capsys) -> None:
    """CLI: код 0 і файл звіту; код 2 і stderr для пошкодженого CSV."""
    output = tmp_path / "out" / "summary.txt"
    assert main([str(data_dir / "vet_visits.csv"), str(output)]) == EXIT_OK
    assert "Візити за видами тварин" in output.read_text(encoding="utf-8")

    broken_output = tmp_path / "broken.txt"
    assert main([str(data_dir / "broken.csv"), str(broken_output)]) == EXIT_DATA_ERROR
    assert "Помилка даних: Рядок 3" in capsys.readouterr().err
    assert not broken_output.exists()
