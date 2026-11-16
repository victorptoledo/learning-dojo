"""A small evaluation harness: scorers, an LLM judge, reports and a regression gate."""

from collections.abc import Callable, Mapping, Sequence
from dataclasses import dataclass, field

from llm import Model


@dataclass(frozen=True)
class Case:
    id: str
    input: str
    expected: str
    tags: tuple[str, ...] = ()


@dataclass(frozen=True)
class Score:
    value: float
    reason: str = ""


type Scorer = Callable[[str, Case], Score]


@dataclass(frozen=True)
class CaseResult:
    case_id: str
    output: str | None
    scores: dict[str, Score]
    passed: bool
    tags: tuple[str, ...] = ()


@dataclass(frozen=True)
class Comparison:
    regressions: list[str]
    fixes: list[str]
    delta: float
    ok: bool
    reasons: list[str] = field(default_factory=list)


def exact_match(output: str, case: Case) -> Score:
    raise NotImplementedError("todo: write exact_match()")


def contains_all(keywords: Sequence[str]) -> Scorer:
    raise NotImplementedError("todo: write contains_all()")


def judge(model: Model, rubric: str) -> Scorer:
    raise NotImplementedError("todo: write judge()")


class Report:
    def __init__(self, results: Sequence[CaseResult]) -> None:
        self.results = list(results)

    def pass_rate(self, tag: str | None = None) -> float:
        raise NotImplementedError("todo: write Report.pass_rate()")

    def mean(self, scorer: str) -> float:
        raise NotImplementedError("todo: write Report.mean()")

    def by_tag(self) -> dict[str, float]:
        raise NotImplementedError("todo: write Report.by_tag()")

    def failures(self) -> list[str]:
        raise NotImplementedError("todo: write Report.failures()")


def run_suite(
    system: Callable[[str], str],
    cases: Sequence[Case],
    scorers: Mapping[str, Scorer],
    thresholds: Mapping[str, float] | None = None,
) -> Report:
    raise NotImplementedError("todo: write run_suite()")


def compare(
    baseline: Report,
    candidate: Report,
    tolerance: float = 0.0,
    critical_tags: Sequence[str] = (),
) -> Comparison:
    raise NotImplementedError("todo: write compare()")
