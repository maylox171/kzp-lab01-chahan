"""Тести функцій аналізу."""

from datetime import date

import pytest

from kzp_lab06.analysis import (
    THRESHOLD,
    average_cost,
    count_by_species,
    make_summary,
    most_expensive,
    select_expensive,
    total_revenue,
    unique_owners,
    urgent_count,
)
from kzp_lab06.loader import load_records


def _record(animal: str, species: str, owner: str, cost: float, urgent: bool = False) -> dict:
    """Створює типізований запис для тесту."""
    return {
        "animal": animal,
        "species": species,
        "owner": owner,
        "date": date(2026, 3, 1),
        "diagnosis": "Огляд",
        "cost": cost,
        "visit_type": "Огляд",
        "urgent": urgent,
    }


def test_indicators_on_control_file(data_dir) -> None:
    """Перевіряє показники ЛР1 на контрольному CSV (збігаються з Java-звітом)."""
    records = load_records(data_dir / "vet_visits.csv")

    assert total_revenue(records) == pytest.approx(5700.5)
    assert average_cost(records) == pytest.approx(950.0833, rel=1e-4)
    assert urgent_count(records) == 3
    assert most_expensive(records)["animal"] == "Рекс"


def test_selection_is_strictly_greater_than_threshold(data_dir) -> None:
    """Граничне значення 1000.0 не потрапляє у відбір cost > 1000.0."""
    records = load_records(data_dir / "vet_visits.csv")
    selected = select_expensive(records)

    assert THRESHOLD == 1000.0
    assert [record["animal"] for record in selected] == ["Рекс", "Ласка"]
    assert all(record["animal"] != "Джек" for record in selected)


def test_empty_selection_result() -> None:
    """Високий поріг дає порожній результат відбору."""
    records = [_record("Барсік", "Кіт", "Іваненко Олена", 450.5)]
    assert select_expensive(records, threshold=5000.0) == []


def test_duplicates_in_owners_and_species() -> None:
    """Дубльовані власники зводяться множиною, види рахуються словником."""
    records = [
        _record("Барсік", "Кіт", "Іваненко Олена", 450.5),
        _record("Кеша", "Папуга", "Іваненко Олена", 200.0),
        _record("Мурчик", "Кіт", "Бондар Ірина", 350.0),
    ]

    assert unique_owners(records) == {"Іваненко Олена", "Бондар Ірина"}
    assert count_by_species(records) == {"Кіт": 2, "Папуга": 1}
    assert list(count_by_species(records)) == sorted(count_by_species(records))


def test_empty_input_gives_zero_indicators() -> None:
    """Порожній вхід: нулі, порожні колекції й відсутній найдорожчий візит."""
    summary = make_summary([])

    assert summary["count"] == 0
    assert summary["average_cost"] == 0.0
    assert summary["total_revenue"] == 0
    assert summary["most_expensive"] is None
    assert summary["species_counts"] == {}
    assert summary["owners"] == set()


def test_make_summary_accepts_args_defaults_and_kwargs() -> None:
    """Перевіряє *metrics, типовий threshold і **labels."""
    records = [
        _record("Рекс", "Собака", "Петренко, Олег", 2500.0, True),
        _record("Ласка", "Тхір", "Коваль Андрій", 1200.0, True),
        _record("Барсік", "Кіт", "Іваненко Олена", 450.5),
    ]

    default = make_summary(records, "cost", cost="Сума дорогих")
    custom = make_summary(records, "cost", threshold=2000.0)

    assert default["Сума дорогих"] == pytest.approx(3700.0)
    assert default["threshold"] == THRESHOLD
    assert custom["cost"] == pytest.approx(2500.0)


def test_unknown_metric_is_rejected() -> None:
    """Невідома метрика дає ValueError, а не неповний звіт."""
    with pytest.raises(ValueError, match="Невідома метрика"):
        make_summary([_record("Барсік", "Кіт", "Іваненко Олена", 1.0)], "weight")
