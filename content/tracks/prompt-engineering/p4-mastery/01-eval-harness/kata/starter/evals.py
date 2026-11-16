"""Grade recorded responses, compare two prompt versions and decide whether one may ship."""

from collections.abc import Iterable, Mapping, Sequence
from dataclasses import dataclass

GRADERS = ("exact", "contains", "regex", "json_keys")


@dataclass(frozen=True)
class Case:
    id: str
    input: str
    expected: str
    grader: str
    tags: tuple[str, ...] = ()


@dataclass(frozen=True)
class Report:
    passed: list[str]
    failed: list[str]
    by_tag: dict[str, tuple[int, int]]

    @property
    def pass_rate(self) -> float:
        total = len(self.passed) + len(self.failed)
        return len(self.passed) / total if total else 0.0


@dataclass(frozen=True)
class Comparison:
    regressions: list[str]
    fixes: list[str]
    delta: float


def load_cases(text: str) -> list[Case]:
    """Cases from JSON Lines, with line-numbered errors."""
    raise NotImplementedError("todo: write load_cases()")


def grade(case: Case, response: str) -> bool:
    """Apply the case's grader to one response."""
    raise NotImplementedError("todo: write grade()")


def run(cases: Sequence[Case], responses: Mapping[str, str]) -> Report:
    """Grade every case; a missing response fails."""
    raise NotImplementedError("todo: write run()")


def compare(baseline: Report, candidate: Report) -> Comparison:
    """Regressions, fixes and the change in pass rate."""
    raise NotImplementedError("todo: write compare()")


def may_ship(
    baseline: Report,
    candidate: Report,
    *,
    critical: Iterable[str] = (),
    max_tag_drop: float = 0.0,
) -> list[str]:
    """The reasons that block shipping the candidate; [] means it may ship."""
    raise NotImplementedError("todo: write may_ship()")
