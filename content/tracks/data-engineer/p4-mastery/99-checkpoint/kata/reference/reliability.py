"""Freshness objectives, incidents, backfills and error budgets of the data products."""

import math
from datetime import date, datetime, timedelta
from fractions import Fraction

from governance import Lineage


def freshness(lineage: Lineage, loaded_through: dict[str, datetime],
              now: datetime) -> dict[str, str]:
    """ok, stale or unknown for every product with an objective, in lineage order."""
    report = {}
    for name in lineage.order():
        model = lineage.models[name]
        if not model.product or model.freshness_hours is None:
            continue
        chain = [name, *lineage.upstream(name)]
        if any(part not in loaded_through for part in chain):
            report[name] = "unknown"
            continue
        oldest = min(loaded_through[part] for part in chain)
        within = now - oldest <= timedelta(hours=model.freshness_hours)
        report[name] = "ok" if within else "stale"
    return report


def blast_radius(lineage: Lineage, broken: str) -> dict:
    """What a broken model affects, and whom to tell."""
    affected = [model for model in lineage.order()
                if model == broken or model in set(lineage.downstream(broken))]
    products = [model for model in affected if lineage.models[model].product]
    owners = sorted({lineage.models[model].owner for model in products})
    return {"models": affected, "products": products, "owners": owners}


def backfill_plan(lineage: Lineage, changed: str, start: date,
                  end: date) -> list[tuple[str, date]]:
    """(model, day) runs to rebuild changed and its downstream models, oldest day first."""
    if start > end:
        raise ValueError("start is after end")
    models = blast_radius(lineage, changed)["models"]
    plan, day = [], start
    while day <= end:
        plan.extend((model, day) for model in models)
        day += timedelta(days=1)
    return plan


def error_budget(days_met: list[bool], objective: float) -> dict:
    """The SLI, the days the objective allows to miss, the days missed and what remains."""
    if not days_met:
        raise ValueError("no days to measure")
    if not 0 < objective <= 1:
        raise ValueError("the objective must be in (0, 1]")
    days = len(days_met)
    missed = days_met.count(False)
    allowed = math.floor(days * (1 - Fraction(str(objective))))
    return {"sli": (days - missed) / days, "allowed": allowed, "missed": missed,
            "remaining": allowed - missed}
