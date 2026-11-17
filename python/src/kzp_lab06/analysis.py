"""Обчислення над записами ветклініки (без читання файлів і друку)."""

from collections.abc import Iterable

THRESHOLD = 1000.0
"""Поріг відбору дорогих візитів: відбираються записи з cost > THRESHOLD (строго)."""

Record = dict[str, object]


def cost_of(record: Record) -> float:
    """Повертає вартість запису як float."""
    return float(record["cost"])  # type: ignore[arg-type]


def select_expensive(records: Iterable[Record], threshold: float = THRESHOLD) -> list[Record]:
    """Відбирає візити з вартістю строго більшою за поріг.

    Результат упорядковано за вартістю (спадання), потім за кличкою.
    """
    selected = [record for record in records if cost_of(record) > threshold]
    return sorted(selected, key=lambda record: (-cost_of(record), str(record["animal"])))


def count_by_species(records: list[Record]) -> dict[str, int]:
    """Рахує кількість візитів за видом тварини у стабільному порядку ключів."""
    species = {str(record["species"]) for record in records}
    return {
        name: sum(1 for record in records if record["species"] == name) for name in sorted(species)
    }


def unique_owners(records: Iterable[Record]) -> set[str]:
    """Повертає множину унікальних власників."""
    return {str(record["owner"]) for record in records}


def average_cost(records: list[Record]) -> float:
    """Обчислює середню вартість візиту; для порожнього входу — 0.0."""
    return sum(cost_of(record) for record in records) / len(records) if records else 0.0


def most_expensive(records: Iterable[Record]) -> Record | None:
    """Повертає найдорожчий візит (перший з однаковою вартістю) або None."""
    return max(records, key=cost_of, default=None)


def make_summary(
    records: list[Record],
    *metrics: str,
    threshold: float = THRESHOLD,
    **labels: str,
) -> dict[str, object]:
    """Формує підсумок звіту та довільні метрики для відібраних записів.

    *metrics — назви числових полів, суми яких рахуються для відібраних записів;
    threshold — типовий поріг відбору; **labels — підписи метрик у результаті.
    Невідома метрика відхиляється ValueError; порожній відбір дає суму 0.
    """
    selected = select_expensive(records, threshold)
    result: dict[str, object] = {
        "count": len(records),
        "average_cost": average_cost(records),
        "most_expensive": most_expensive(records),
        "threshold": threshold,
        "selected": selected,
        "species_counts": count_by_species(records),
        "owners": unique_owners(records),
    }
    for metric in metrics:
        if any(metric not in record for record in records):
            raise ValueError(f"Невідома метрика: {metric}")
        result[labels.get(metric, metric)] = sum(
            float(record[metric])  # type: ignore[arg-type]
            for record in selected
        )
    return result
