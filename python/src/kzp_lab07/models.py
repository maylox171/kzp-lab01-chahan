"""Об'єктна модель ветклініки: сутність, підтипи, протокол і незмінні підсумки."""

import datetime as dt
import math
from dataclasses import dataclass
from typing import Protocol

URGENT_RATE = 1.20
"""Коефіцієнт ургентної надбавки (той самий, що в Java UrgentVisit.URGENT_RATE)."""


class Costed(Protocol):
    """Протокол об'єктів, що вміють обчислити підсумкову вартість."""

    @property
    def cost(self) -> float:
        """Повертає базову вартість."""
        ...

    def final_cost(self) -> float:
        """Повертає підсумкову вартість з урахуванням надбавок."""
        ...


def _require_text(value: object, name: str) -> str:
    """Перевіряє, що значення — непорожній рядок, і нормалізує пробіли по краях."""
    if not isinstance(value, str) or not value.strip():
        raise ValueError(f"Поле {name} не може бути порожнім")
    return value.strip()


class VetVisit:
    """Базовий візит до ветклініки зі спільним перевіреним станом.

    Інваріанти: текстові поля непорожні, date — datetime.date, cost — скінченне число ≥ 0.
    Підтипи визначають final_cost() і ознаку urgent.
    """

    urgent: bool = False
    """Ознака ургентності; перевизначається підтипом."""

    def __init__(
        self,
        animal: str,
        species: str,
        owner: str,
        visit_date: dt.date,
        diagnosis: str,
        cost: float,
    ) -> None:
        """Створює візит; кожне значення проходить через setter-валідацію."""
        self.animal = animal
        self.species = species
        self.owner = owner
        self.date = visit_date
        self.diagnosis = diagnosis
        self.cost = cost

    @property
    def animal(self) -> str:
        """Повертає кличку тварини."""
        return self._animal

    @animal.setter
    def animal(self, value: str) -> None:
        """Встановлює непорожню кличку."""
        self._animal = _require_text(value, "animal")

    @property
    def species(self) -> str:
        """Повертає вид тварини."""
        return self._species

    @species.setter
    def species(self, value: str) -> None:
        """Встановлює непорожній вид."""
        self._species = _require_text(value, "species")

    @property
    def owner(self) -> str:
        """Повертає власника."""
        return self._owner

    @owner.setter
    def owner(self, value: str) -> None:
        """Встановлює непорожнього власника."""
        self._owner = _require_text(value, "owner")

    @property
    def diagnosis(self) -> str:
        """Повертає діагноз."""
        return self._diagnosis

    @diagnosis.setter
    def diagnosis(self, value: str) -> None:
        """Встановлює непорожній діагноз."""
        self._diagnosis = _require_text(value, "diagnosis")

    @property
    def date(self) -> dt.date:
        """Повертає дату візиту."""
        return self._date

    @date.setter
    def date(self, value: dt.date) -> None:
        """Встановлює дату візиту (лише datetime.date)."""
        if not isinstance(value, dt.date):
            raise ValueError("Поле date має бути датою")
        self._date = value

    @property
    def cost(self) -> float:
        """Повертає базову вартість візиту."""
        return self._cost

    @cost.setter
    def cost(self, value: float) -> None:
        """Встановлює скінченну невід'ємну вартість; 0 допустимий (граничне значення)."""
        # bool є підкласом int, але вартістю не є
        if isinstance(value, bool) or not isinstance(value, int | float):
            raise ValueError("Поле cost має бути числом")
        if not math.isfinite(value) or value < 0:
            raise ValueError("Поле cost не може бути від'ємним")
        # Записуємо внутрішнє поле, щоб setter не викликав сам себе
        self._cost = float(value)

    @property
    def key(self) -> tuple[str, dt.date]:
        """Повертає ключ візиту: власник і дата (як у Java ЛР5)."""
        return (self.owner, self.date)

    def final_cost(self) -> float:
        """Оголошує спільну операцію для підтипів."""
        raise NotImplementedError

    def _state(self) -> tuple[object, ...]:
        """Повертає повний стан для порівняння."""
        return (
            self.animal,
            self.species,
            self.owner,
            self.date,
            self.diagnosis,
            self.cost,
        )

    def __str__(self) -> str:
        """Повертає коротке подання для читача."""
        kind = "ургентний" if self.urgent else "плановий"
        return f"{self.animal} ({self.species}), {self.date}, {kind}: {self.final_cost():.2f}"

    def __repr__(self) -> str:
        """Повертає діагностичне подання."""
        return (
            f"{type(self).__name__}(animal={self.animal!r}, species={self.species!r}, "
            f"owner={self.owner!r}, visit_date={self.date!r}, diagnosis={self.diagnosis!r}, "
            f"cost={self.cost!r})"
        )

    def __eq__(self, other: object) -> bool:
        """Порівнює візити одного підтипу за повним станом."""
        if not isinstance(other, VetVisit):
            return NotImplemented
        return type(self) is type(other) and self._state() == other._state()

    # Об'єкт змінюваний (є setter-и), тому не хешується
    __hash__ = None  # type: ignore[assignment]


class RoutineVisit(VetVisit):
    """Плановий візит: підсумкова вартість дорівнює базовій."""

    urgent = False

    def final_cost(self) -> float:
        """Повертає базову вартість без надбавки."""
        return self.cost


class UrgentVisit(VetVisit):
    """Ургентний візит: до базової вартості додається надбавка 20 %."""

    urgent = True

    def final_cost(self) -> float:
        """Повертає вартість з ургентною надбавкою."""
        return self.cost * URGENT_RATE


@dataclass(frozen=True)
class SpeciesStat:
    """Незмінний підсумок візитів за одним видом тварини."""

    species: str
    count: int
    total_final_cost: float


@dataclass(frozen=True)
class ReportSummary:
    """Незмінний підсумок звіту ветклініки."""

    count: int
    average_cost: float
    urgent_count: int
    total_final_cost: float
    threshold: float
    most_expensive: VetVisit | None
    selected: tuple[VetVisit, ...]
    by_species: tuple[SpeciesStat, ...]
    owners: frozenset[str]
