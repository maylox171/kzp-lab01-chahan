"""Тести читання та перетворення CSV."""

from datetime import date

import pytest

from kzp_lab06.errors import DataError
from kzp_lab06.loader import load_records

HEADER = "animal,species,owner,date,diagnosis,cost,visit_type,urgent\n"


def test_loads_correct_file_with_comma_and_quotes(data_dir) -> None:
    """Перевіряє коректний CSV, текст із комою та подвоєні лапки."""
    records = load_records(data_dir / "vet_visits.csv")

    assert len(records) == 6
    rex = records[1]
    assert rex["owner"] == "Петренко, Олег"
    assert rex["diagnosis"] == 'Перелом лапи, "складний"'
    assert rex["cost"] == 2500.0
    assert rex["urgent"] is True
    assert rex["date"] == date(2026, 3, 2)


def test_header_only_gives_empty_list(data_dir) -> None:
    """Перевіряє правило порожнього входу: лише заголовок — порожній список."""
    assert load_records(data_dir / "empty.csv") == []


def test_completely_empty_file_is_error(write_csv) -> None:
    """Файл без заголовка відхиляється DataError."""
    with pytest.raises(DataError, match="порожній"):
        load_records(write_csv(""))


def test_bad_number_has_row_number_and_cause(write_csv) -> None:
    """Хибне число дає номер рядка та першопричину ValueError."""
    path = write_csv(HEADER + "Рижик,Кіт,Мельник Петро,2026-03-08,Огляд,дорого,Огляд,false\n")
    with pytest.raises(DataError, match="Рядок 2: поле cost") as info:
        load_records(path)
    assert isinstance(info.value.__cause__, ValueError)


def test_negative_cost_is_rejected(write_csv) -> None:
    """Від'ємна вартість заборонена."""
    path = write_csv(HEADER + "Мурка,Кіт,Сидоренко Марія,2026-03-07,Травма,-150,Травма,true\n")
    with pytest.raises(DataError, match="від'ємним"):
        load_records(path)


def test_missing_column_is_rejected(write_csv) -> None:
    """Відсутній обов'язковий стовпець називається в повідомленні."""
    path = write_csv("animal,species,owner,date,diagnosis,visit_type,urgent\n")
    with pytest.raises(DataError, match="cost"):
        load_records(path)


def test_missing_value_in_row_is_rejected(write_csv) -> None:
    """Рядок з меншою кількістю значень дає DataError із номером рядка."""
    path = write_csv(HEADER + "Тузік,Собака,Огляд\n")
    with pytest.raises(DataError, match="Рядок 2: відсутнє значення"):
        load_records(path)


def test_empty_text_bad_date_and_bad_boolean(write_csv) -> None:
    """Порожнє текстове поле, хибна дата й хибний urgent відхиляються."""
    empty = write_csv(HEADER + "Бім,Собака, ,2026-03-01,Огляд,10,Огляд,false\n", "a.csv")
    bad_date = write_csv(
        HEADER + "Бім,Собака,Лисенко Ганна,2026-13-40,Огляд,10,Огляд,false\n", "b.csv"
    )
    bad_bool = write_csv(
        HEADER + "Бім,Собака,Лисенко Ганна,2026-03-01,Огляд,10,Огляд,так\n", "c.csv"
    )
    with pytest.raises(DataError, match="owner порожнє"):
        load_records(empty)
    with pytest.raises(DataError, match="date"):
        load_records(bad_date)
    with pytest.raises(DataError, match="urgent"):
        load_records(bad_bool)


def test_missing_file_keeps_os_error(tmp_path) -> None:
    """Відсутній файл перетворюється на DataError з причиною OSError."""
    with pytest.raises(DataError) as info:
        load_records(tmp_path / "missing.csv")
    assert isinstance(info.value.__cause__, OSError)
