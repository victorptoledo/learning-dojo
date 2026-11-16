"""A small evaluation harness: scorers, an LLM judge, reports and a regression gate."""

import json
from collections.abc import Callable, Mapping, Sequence
from dataclasses import dataclass, field

from llm import Message, Model, Request


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


def _normalise(text: str) -> str:
    return " ".join(text.casefold().split()).rstrip(".!?")


def exact_match(output: str, case: Case) -> Score:
    return Score(1.0 if _normalise(output) == _normalise(case.expected) else 0.0)


def contains_all(keywords: Sequence[str]) -> Scorer:
    wanted = list(keywords)

    def scorer(output: str, case: Case) -> Score:
        if not wanted:
            return Score(1.0)
        text = output.casefold()
        missing = [keyword for keyword in wanted if keyword.casefold() not in text]
        reason = "missing: " + ", ".join(missing) if missing else ""
        return Score((len(wanted) - len(missing)) / len(wanted), reason)

    return scorer


def judge(model: Model, rubric: str) -> Scorer:
    def scorer(output: str, case: Case) -> Score:
        prompt = f"Input: {case.input}\nExpected: {case.expected}\nOutput: {output}"
        response = model.complete(Request((Message("user", prompt),), system=rubric))
        try:
            verdict = json.loads(response.text)
        except json.JSONDecodeError:
            return Score(0.0, "invalid judge reply")
        score = verdict.get("score") if isinstance(verdict, dict) else None
        if not isinstance(score, int) or isinstance(score, bool) or not 1 <= score <= 5:
            return Score(0.0, "invalid judge reply")
        return Score((score - 1) / 4, str(verdict.get("reason", "")))

    return scorer


class Report:
    def __init__(self, results: Sequence[CaseResult]) -> None:
        self.results = list(results)

    def pass_rate(self, tag: str | None = None) -> float:
        chosen = [r for r in self.results if tag is None or tag in r.tags]
        return sum(r.passed for r in chosen) / len(chosen) if chosen else 0.0

    def mean(self, scorer: str) -> float:
        values = [r.scores[scorer].value for r in self.results if scorer in r.scores]
        return sum(values) / len(values) if values else 0.0

    def by_tag(self) -> dict[str, float]:
        tags = sorted({tag for result in self.results for tag in result.tags})
        return {tag: self.pass_rate(tag) for tag in tags}

    def failures(self) -> list[str]:
        return [r.case_id for r in self.results if not r.passed]


def run_suite(
    system: Callable[[str], str],
    cases: Sequence[Case],
    scorers: Mapping[str, Scorer],
    thresholds: Mapping[str, float] | None = None,
) -> Report:
    limits = thresholds or {}
    results = []
    for case in cases:
        try:
            output: str | None = system(case.input)
        except Exception as error:
            reason = f"error: {type(error).__name__}: {error}"
            scores = {name: Score(0.0, reason) for name in scorers}
            results.append(CaseResult(case.id, None, scores, False, case.tags))
            continue
        scores = {name: scorer(output, case) for name, scorer in scorers.items()}
        passed = all(score.value >= limits.get(name, 1.0) for name, score in scores.items())
        results.append(CaseResult(case.id, output, scores, passed, case.tags))
    return Report(results)


def compare(
    baseline: Report,
    candidate: Report,
    tolerance: float = 0.0,
    critical_tags: Sequence[str] = (),
) -> Comparison:
    before = {r.case_id: r for r in baseline.results}
    after = {r.case_id: r for r in candidate.results}
    common = [case_id for case_id in before if case_id in after]
    regressions = [i for i in common if before[i].passed and not after[i].passed]
    fixes = [i for i in common if not before[i].passed and after[i].passed]
    delta = candidate.pass_rate() - baseline.pass_rate()
    reasons = []
    if delta < -tolerance:
        reasons.append(f"pass rate dropped by {-delta:.3f}")
    critical = [i for i in regressions if set(after[i].tags) & set(critical_tags)]
    if critical:
        reasons.append("critical regressions: " + ", ".join(critical))
    return Comparison(regressions, fixes, delta, not reasons, reasons)
