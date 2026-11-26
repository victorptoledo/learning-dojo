"""Freshness objectives, incidents, backfills and error budgets of the data products."""

from datetime import date, datetime

from governance import Lineage


def freshness(lineage: Lineage, loaded_through: dict[str, datetime],
              now: datetime) -> dict[str, str]:
    """ok, stale or unknown for every product with an objective, in lineage order."""
    raise NotImplementedError


def blast_radius(lineage: Lineage, broken: str) -> dict:
    """What a broken model affects, and whom to tell."""
    raise NotImplementedError


def backfill_plan(lineage: Lineage, changed: str, start: date,
                  end: date) -> list[tuple[str, date]]:
    """(model, day) runs to rebuild changed and its downstream models, oldest day first."""
    raise NotImplementedError


def error_budget(days_met: list[bool], objective: float) -> dict:
    """The SLI, the days the objective allows to miss, the days missed and what remains."""
    raise NotImplementedError
