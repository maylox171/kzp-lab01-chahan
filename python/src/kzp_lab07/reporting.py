"""Текстовий звіт: частина ЛР6 без змін + розділ об'єктної моделі ЛР7."""

from pathlib import Path

from .models import URGENT_RATE, ReportSummary, VetVisit


def _describe(visit: VetVisit) -> str:
    """Повертає опис візиту у форматі звіту ЛР6."""
    return (
        f"{visit.animal} ({visit.species}, власник {visit.owner}, {visit.date}) — {visit.cost:.2f}"
    )


def format_base_report(summary: ReportSummary) -> str:
    """Формує частину звіту, сумісну зі звітом ЛР6 символ у символ."""
    most = summary.most_expensive
    lines = [
        "Звіт ветклініки (варіант 21)",
        f"Кількість візитів: {summary.count}",
        f"Середня вартість візиту: {summary.average_cost:.2f}",
        f"Найдорожчий візит: {_describe(most) if most else 'немає'}",
        f"Візити з вартістю > {summary.threshold:.2f}: {len(summary.selected)}",
    ]
    lines.extend(f"  - {_describe(visit)}" for visit in summary.selected)
    if not summary.selected:
        lines.append("  (немає записів)")
    lines.append("Кількість візитів за видами:")
    lines.extend(f"  {stat.species}: {stat.count}" for stat in summary.by_species)
    lines.append(f"Власники ({len(summary.owners)}): {'; '.join(sorted(summary.owners))}")
    return "\n".join(lines) + "\n"


def format_report(summary: ReportSummary) -> str:
    """Формує повний детермінований звіт ЛР7."""
    lines = [
        "",
        f"Ургентних візитів (за діагнозом): {summary.urgent_count}",
        f"Підсумкова вартість з ургентною надбавкою (×{URGENT_RATE:.2f}): "
        f"{summary.total_final_cost:.2f}",
        "Візити за видами тварин (кількість / підсумкова вартість):",
    ]
    lines.extend(
        f"  {stat.species}: {stat.count} / {stat.total_final_cost:.2f}"
        for stat in summary.by_species
    )
    return format_base_report(summary) + "\n".join(lines) + "\n"


def write_report(path: Path, text: str) -> None:
    """Створює каталог і записує звіт у файл UTF-8."""
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="\n") as target:
        target.write(text)
