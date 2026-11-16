"""Data quality checks for a batch of rows, in the spirit of dbt tests and Great Expectations."""

from collections.abc import Callable, Iterable
from dataclasses import dataclass
from datetime import datetime, timedelta

Row = dict[str, object]
SAMPLE_SIZE = 3


@dataclass(frozen=True)
class CheckResult:
    name: str
    severity: str  # "error" or "warn"
    passed: bool
    failing: int  # failing rows; 1 or 0 for a check on the whole table
    sample: tuple[int, ...]  # indexes of the first failing rows


@dataclass(frozen=True)
class Check:
    name: str
    severity: str
    evaluate: Callable[[list[Row]], CheckResult]


@dataclass(frozen=True)
class Report:
    results: tuple[CheckResult, ...]

    @property
    def passed(self) -> bool:
        """True when no check with severity "error" failed; warnings do not block."""
        raise NotImplementedError("todo: write Report.passed")

    @property
    def failures(self) -> list[CheckResult]:
        raise NotImplementedError("todo: write Report.failures")

    @property
    def warnings(self) -> list[CheckResult]:
        raise NotImplementedError("todo: write Report.warnings")

    def summary(self) -> str:
        raise NotImplementedError("todo: write Report.summary()")


class QualityError(Exception):
    def __init__(self, report: Report):
        super().__init__(report.summary())
        self.report = report


def not_null(column: str, *, severity: str = "error") -> Check:
    raise NotImplementedError("todo: write not_null()")


def unique(*columns: str, severity: str = "error") -> Check:
    raise NotImplementedError("todo: write unique()")


def accepted_values(column: str, values: Iterable[object], *, severity: str = "error") -> Check:
    raise NotImplementedError("todo: write accepted_values()")


def in_range(column: str, low=None, high=None, *, severity: str = "error") -> Check:
    raise NotImplementedError("todo: write in_range()")


def relationship(column: str, parents: Iterable[Row], parent_column: str, *,
                 severity: str = "error") -> Check:
    raise NotImplementedError("todo: write relationship()")


def row_count(low: int, high: int, *, severity: str = "error") -> Check:
    raise NotImplementedError("todo: write row_count()")


def freshness(column: str, max_age: timedelta, now: datetime, *,
              severity: str = "error") -> Check:
    raise NotImplementedError("todo: write freshness()")


def run_checks(rows: list[Row], checks: Iterable[Check]) -> Report:
    raise NotImplementedError("todo: write run_checks()")


def assert_quality(rows: list[Row], checks: Iterable[Check]) -> Report:
    """The report when the batch may be published; raises QualityError when it may not."""
    raise NotImplementedError("todo: write assert_quality()")
