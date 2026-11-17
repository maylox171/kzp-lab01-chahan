"""Форматування й запис текстового звіту."""

from pathlib import Path


def _describe(record: dict[str, object]) -> str:
    """Повертає короткий опис візиту для звіту."""
    return (
        f"{record['animal']} ({record['species']}, власник {record['owner']}, "
        f"{record['date']}) — {float(record['cost']):.2f}"  # type: ignore[arg-type]
    )


def format_report(summary: dict[str, object]) -> str:
    """Форматує підсумок у детермінований текстовий звіт (UTF-8, роздільник \\n)."""
    most = summary["most_expensive"]
    selected: list[dict[str, object]] = summary["selected"]  # type: ignore[assignment]
    species_counts: dict[str, int] = summary["species_counts"]  # type: ignore[assignment]
    owners: set[str] = summary["owners"]  # type: ignore[assignment]
    lines = [
        "Звіт ветклініки (варіант 21)",
        f"Кількість візитів: {summary['count']}",
        f"Середня вартість візиту: {summary['average_cost']:.2f}",
        f"Найдорожчий візит: {_describe(most) if most else 'немає'}",  # type: ignore[arg-type]
        f"Візити з вартістю > {summary['threshold']:.2f}: {len(selected)}",
    ]
    lines.extend(f"  - {_describe(record)}" for record in selected)
    if not selected:
        lines.append("  (немає записів)")
    lines.append("Кількість візитів за видами:")
    lines.extend(f"  {name}: {count}" for name, count in sorted(species_counts.items()))
    lines.append(f"Власники ({len(owners)}): {'; '.join(sorted(owners))}")
    return "\n".join(lines) + "\n"


def write_report(path: Path, text: str) -> None:
    """Створює каталог і записує звіт у файл UTF-8."""
    path.parent.mkdir(parents=True, exist_ok=True)
    # with гарантує закриття файла навіть у разі помилки запису
    with path.open("w", encoding="utf-8", newline="\n") as target:
        target.write(text)
