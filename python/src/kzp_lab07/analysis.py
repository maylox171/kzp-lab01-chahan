"""Обчислення над об'єктами-візитами через спільний протокол (без перевірок підтипу)."""

from collections.abc import Iterable, Sequence

from .models import Costed, ReportSummary, SpeciesStat, VetVisit

THRESHOLD = 1000.0
"""Поріг відбору дорогих візитів: cost > THRESHOLD (строго), як у ЛР6."""


def order_key(visit: VetVisit) -> tuple[float, str]:
    """Ключ впорядкування: базова вартість за спаданням, потім кличка."""
    return (-visit.cost, visit.animal)


def select_expensive(visits: Iterable[VetVisit], threshold: float = THRESHOLD) -> list[VetVisit]:
    """Відбирає візити з базовою вартістю строго більшою за поріг."""
    return sorted((visit for visit in visits if visit.cost > threshold), key=order_key)


def most_expensive(visits: Iterable[VetVisit]) -> VetVisit | None:
    """Повертає візит із найбільшою базовою вартістю або None."""
    return max(visits, key=lambda visit: visit.cost, default=None)


def best_by_final_cost(items: Iterable[Costed]) -> Costed | None:
    """Повертає об'єкт із найбільшою підсумковою вартістю (качина типізація)."""
    # Функція знає лише про final_cost(), тому новий підтип не потребує змін тут
    return max(items, key=lambda item: item.final_cost(), default=None)


def total_final_cost(items: Iterable[Costed]) -> float:
    """Сумує підсумкові вартості через спільний протокол."""
    return sum(item.final_cost() for item in items)


def visits_by_species(visits: Sequence[VetVisit]) -> tuple[SpeciesStat, ...]:
    """Підсумок «візити за видами тварин» у відсортованому порядку видів."""
    return tuple(
        SpeciesStat(
            species=name,
            count=sum(1 for visit in visits if visit.species == name),
            total_final_cost=total_final_cost(v for v in visits if v.species == name),
        )
        for name in sorted({visit.species for visit in visits})
    )


def make_summary(visits: Sequence[VetVisit], threshold: float = THRESHOLD) -> ReportSummary:
    """Формує незмінний підсумок звіту."""
    revenue = sum(visit.cost for visit in visits)
    return ReportSummary(
        count=len(visits),
        total_revenue=revenue,
        average_cost=revenue / len(visits) if visits else 0.0,
        urgent_count=sum(1 for visit in visits if visit.urgent),
        total_final_cost=total_final_cost(visits),
        threshold=threshold,
        most_expensive=most_expensive(visits),
        selected=tuple(select_expensive(visits, threshold)),
        by_species=visits_by_species(visits),
        owners=frozenset(visit.owner for visit in visits),
    )
