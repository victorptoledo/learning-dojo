"""Data quality checks for a batch of rows, in the spirit of dbt tests and Great Expectations."""

from collections import Counter
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
        return all(r.passed or r.severity == "warn" for r in self.results)

    @property
    def failures(self) -> list[CheckResult]:
        return [r for r in self.results if not r.passed and r.severity == "error"]

    @property
    def warnings(self) -> list[CheckResult]:
        return [r for r in self.results if not r.passed and r.severity == "warn"]

    def summary(self) -> str:
        lines = []
        for result in self.results:
            if result.passed:
                lines.append(f"PASS {result.name}")
                continue
            label = "FAIL" if result.severity == "error" else "WARN"
            line = f"{label} {result.name}"
            if result.sample:
                rows = ", ".join(str(i) for i in result.sample)
                line += f": {result.failing} failing, rows {rows}"
            lines.append(line)
        passed = sum(r.passed for r in self.results)
        lines.append(
            f"{passed} passed, {len(self.failures)} failed, {len(self.warnings)} warnings"
        )
        return "\n".join(lines)


class QualityError(Exception):
    def __init__(self, report: Report):
        super().__init__(report.summary())
        self.report = report


def check_severity(severity: str) -> str:
    if severity not in ("error", "warn"):
        raise ValueError(f"unknown severity: {severity!r}")
    return severity


def row_check(name: str, severity: str, bad: Callable[[Row], bool]) -> Check:
    """A check that fails for every row where bad(row) is true."""
    severity = check_severity(severity)

    def evaluate(rows: list[Row]) -> CheckResult:
        failing = [i for i, row in enumerate(rows) if bad(row)]
        return CheckResult(name, severity, not failing, len(failing),
                           tuple(failing[:SAMPLE_SIZE]))

    return Check(name, severity, evaluate)


def table_check(name: str, severity: str, ok: Callable[[list[Row]], bool]) -> Check:
    """A check on the whole batch."""
    severity = check_severity(severity)

    def evaluate(rows: list[Row]) -> CheckResult:
        passed = ok(rows)
        return CheckResult(name, severity, passed, 0 if passed else 1, ())

    return Check(name, severity, evaluate)


def not_null(column: str, *, severity: str = "error") -> Check:
    return row_check(f"not_null({column})", severity, lambda row: row.get(column) is None)


def unique(*columns: str, severity: str = "error") -> Check:
    if not columns:
        raise ValueError("unique needs at least one column")
    name = f"unique({', '.join(columns)})"
    severity = check_severity(severity)

    def key(row: Row) -> tuple | None:
        values = tuple(row.get(c) for c in columns)
        return None if any(v is None for v in values) else values

    def evaluate(rows: list[Row]) -> CheckResult:
        counts = Counter(k for k in map(key, rows) if k is not None)
        failing = [i for i, row in enumerate(rows) if counts.get(key(row), 0) > 1]
        return CheckResult(name, severity, not failing, len(failing),
                           tuple(failing[:SAMPLE_SIZE]))

    return Check(name, severity, evaluate)


def accepted_values(column: str, values: Iterable[object], *, severity: str = "error") -> Check:
    allowed = frozenset(values)

    def bad(row: Row) -> bool:
        value = row.get(column)
        return value is not None and value not in allowed

    return row_check(f"accepted_values({column})", severity, bad)


def in_range(column: str, low=None, high=None, *, severity: str = "error") -> Check:
    def bad(row: Row) -> bool:
        value = row.get(column)
        if value is None:
            return False
        return (low is not None and value < low) or (high is not None and value > high)

    return row_check(f"in_range({column})", severity, bad)


def relationship(column: str, parents: Iterable[Row], parent_column: str, *,
                 severity: str = "error") -> Check:
    known = {parent.get(parent_column) for parent in parents}

    def bad(row: Row) -> bool:
        value = row.get(column)
        return value is not None and value not in known

    return row_check(f"relationship({column} -> {parent_column})", severity, bad)


def row_count(low: int, high: int, *, severity: str = "error") -> Check:
    return table_check(f"row_count({low}..{high})", severity,
                       lambda rows: low <= len(rows) <= high)


def freshness(column: str, max_age: timedelta, now: datetime, *,
              severity: str = "error") -> Check:
    def ok(rows: list[Row]) -> bool:
        stamps = [row.get(column) for row in rows if row.get(column) is not None]
        return bool(stamps) and max(stamps) >= now - max_age

    return table_check(f"freshness({column})", severity, ok)


def run_checks(rows: list[Row], checks: Iterable[Check]) -> Report:
    return Report(tuple(check.evaluate(rows) for check in checks))


def assert_quality(rows: list[Row], checks: Iterable[Check]) -> Report:
    """The report when the batch may be published; raises QualityError when it may not."""
    report = run_checks(rows, checks)
    if not report.passed:
        raise QualityError(report)
    return report
