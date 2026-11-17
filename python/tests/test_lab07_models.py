"""Тести об'єктної моделі ЛР7: властивості, dataclass, спеціальні методи, підтипи, протокол."""

import dataclasses
from datetime import date

import pytest

from kzp_lab07.analysis import best_by_final_cost, order_key, total_final_cost
from kzp_lab07.models import (
    URGENT_RATE,
    ReportSummary,
    RoutineVisit,
    SpeciesStat,
    UrgentVisit,
    VetVisit,
)

DAY = date(2026, 3, 2)


def _routine(cost: float = 450.5, animal: str = "Барсік") -> RoutineVisit:
    """Створює плановий візит для тестів."""
    return RoutineVisit(animal, "Кіт", "Іваненко Олена", DAY, "Огляд", cost, "Огляд")


def _urgent(cost: float = 2500.0, animal: str = "Рекс") -> UrgentVisit:
    """Створює ургентний візит для тестів."""
    return UrgentVisit(animal, "Собака", "Петренко, Олег", DAY, "Перелом", cost, "Хірургія")


def test_cost_setter_accepts_correct_zero_and_boundary_values() -> None:
    """Setter приймає звичайне, нульове та граничне значення."""
    visit = _routine()
    visit.cost = 1000.0
    assert visit.cost == 1000.0
    visit.cost = 0
    assert visit.cost == 0.0
    assert isinstance(visit.cost, float)


@pytest.mark.parametrize("bad", [-0.01, -150, float("nan"), float("inf"), "100", True, None])
def test_cost_setter_rejects_invalid_values(bad: object) -> None:
    """Setter відхиляє від'ємне, нескінченне та нечислове значення."""
    visit = _routine()
    with pytest.raises(ValueError):
        visit.cost = bad  # type: ignore[assignment]
    assert visit.cost == 450.5


def test_text_and_date_setters_validate() -> None:
    """Текстові поля непорожні, дата — лише datetime.date; пробіли нормалізуються."""
    visit = _routine()
    visit.owner = "  Бондар Ірина "
    assert visit.owner == "Бондар Ірина"
    with pytest.raises(ValueError, match="owner"):
        visit.owner = "   "
    with pytest.raises(ValueError, match="date"):
        visit.date = "2026-03-02"  # type: ignore[assignment]
    with pytest.raises(ValueError):
        RoutineVisit("", "Кіт", "Іваненко Олена", DAY, "Огляд", 1.0, "Огляд")


def test_subtypes_have_different_final_cost() -> None:
    """Різні формули спільної операції: ургентний ×1.20 (як у Java)."""
    routine = _routine(1000.0)
    urgent = _urgent(1000.0)

    assert routine.final_cost() == 1000.0
    assert urgent.final_cost() == pytest.approx(1000.0 * URGENT_RATE)
    assert routine.final_cost() != urgent.final_cost()
    assert (routine.urgent, urgent.urgent) == (False, True)


def test_base_class_operation_is_abstract() -> None:
    """Базовий клас лише оголошує final_cost()."""
    with pytest.raises(NotImplementedError):
        VetVisit("Барсік", "Кіт", "Іваненко Олена", DAY, "Огляд", 1.0, "Огляд").final_cost()


def test_equality_repr_and_str() -> None:
    """__eq__ за станом і підтипом, NotImplemented для іншого типу, repr/str."""
    assert _routine() == _routine()
    assert _routine() != _routine(cost=451.0)
    assert _urgent(100.0) != RoutineVisit(
        "Рекс", "Собака", "Петренко, Олег", DAY, "Перелом", 100.0, "Хірургія"
    )
    assert _routine().__eq__("рядок") is NotImplemented
    assert repr(_routine()).startswith("RoutineVisit(animal='Барсік'")
    assert str(_urgent()) == "Рекс (Собака), 2026-03-02, ургентний: 3000.00"
    with pytest.raises(TypeError):
        hash(_routine())


def test_duplicates_are_detected_by_equality() -> None:
    """Дубльований запис рівний оригіналу, тому видаляється порівнянням."""
    visits = [_routine(), _urgent(), _routine()]
    unique: list[VetVisit] = []
    for visit in visits:
        if visit not in unique:
            unique.append(visit)
    assert len(unique) == 2


def test_ordering_by_key() -> None:
    """Сортування через key: вартість за спаданням, потім кличка."""
    visits = [_routine(200.0, "Кеша"), _urgent(2500.0), _routine(200.0, "Аркаша")]
    ordered = sorted(visits, key=order_key)
    assert [visit.animal for visit in ordered] == ["Рекс", "Аркаша", "Кеша"]


def test_summary_dataclasses_are_frozen_and_comparable() -> None:
    """frozen dataclass: порівняння за полями та заборона змін."""
    stat = SpeciesStat("Кіт", 2, 800.5)
    assert stat == SpeciesStat("Кіт", 2, 800.5)
    with pytest.raises(dataclasses.FrozenInstanceError):
        stat.count = 3  # type: ignore[misc]
    summary = ReportSummary(0, 0.0, 0.0, 0, 0.0, 1000.0, None, (), (), frozenset())
    with pytest.raises(dataclasses.FrozenInstanceError):
        summary.count = 1  # type: ignore[misc]


def test_protocol_function_works_with_both_subtypes_and_duck_type() -> None:
    """best_by_final_cost працює з обома підтипами та будь-яким об'єктом з final_cost()."""

    class Fee:
        """Сторонній клас без спільного предка, але з потрібними методами."""

        cost = 5000.0

        def final_cost(self) -> float:
            """Повертає фіксовану вартість."""
            return self.cost

    routine, urgent = _routine(2000.0), _urgent(1800.0)
    assert best_by_final_cost([routine, urgent]) is urgent  # 2160 > 2000
    fee = Fee()
    assert best_by_final_cost([routine, urgent, fee]) is fee
    assert best_by_final_cost([]) is None
    assert total_final_cost([routine, urgent]) == pytest.approx(4160.0)
