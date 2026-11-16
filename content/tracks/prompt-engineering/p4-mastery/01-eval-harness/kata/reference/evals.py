"""Grade recorded responses, compare two prompt versions and decide whether one may ship."""

import json
import re
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
    cases: list[Case] = []
    seen: set[str] = set()
    for number, line in enumerate(text.splitlines(), start=1):
        if not line.strip():
            continue
        try:
            data = json.loads(line)
        except json.JSONDecodeError:
            raise ValueError(f"line {number}: invalid JSON") from None
        if not isinstance(data, dict):
            raise ValueError(f"line {number}: invalid JSON")
        for field in ("id", "input", "expected", "grader"):
            if not isinstance(data.get(field), str) or (field == "id" and not data[field]):
                raise ValueError(f"line {number}: missing field '{field}'")
        tags = data.get("tags", [])
        if not isinstance(tags, list) or not all(isinstance(tag, str) for tag in tags):
            raise ValueError(f"line {number}: missing field 'tags'")
        if data["grader"] not in GRADERS:
            raise ValueError(f"line {number}: unknown grader '{data['grader']}'")
        if data["id"] in seen:
            raise ValueError(f"line {number}: duplicate id '{data['id']}'")
        seen.add(data["id"])
        cases.append(Case(data["id"], data["input"], data["expected"], data["grader"], tuple(tags)))
    return cases


def _normalise(text: str) -> str:
    return " ".join(text.casefold().split())


def grade(case: Case, response: str) -> bool:
    """Apply the case's grader to one response."""
    match case.grader:
        case "exact":
            return _normalise(response) == _normalise(case.expected)
        case "contains":
            return _normalise(case.expected) in _normalise(response)
        case "regex":
            return re.search(case.expected, response, re.IGNORECASE | re.DOTALL) is not None
        case "json_keys":
            try:
                data = json.loads(response.strip())
            except json.JSONDecodeError:
                return False
            keys = [key.strip() for key in case.expected.split(",") if key.strip()]
            return isinstance(data, dict) and all(key in data for key in keys)
    raise ValueError(f"unknown grader '{case.grader}'")


def run(cases: Sequence[Case], responses: Mapping[str, str]) -> Report:
    """Grade every case; a missing response fails."""
    passed: list[str] = []
    failed: list[str] = []
    tags: dict[str, list[int]] = {}
    for case in cases:
        ok = case.id in responses and grade(case, responses[case.id])
        (passed if ok else failed).append(case.id)
        for tag in case.tags:
            counts = tags.setdefault(tag, [0, 0])
            counts[0] += ok
            counts[1] += 1
    by_tag = {tag: (counts[0], counts[1]) for tag, counts in sorted(tags.items())}
    return Report(passed, failed, by_tag)


def compare(baseline: Report, candidate: Report) -> Comparison:
    """Regressions, fixes and the change in pass rate."""
    was_passing = set(baseline.passed)
    was_failing = set(baseline.failed)
    regressions = [case_id for case_id in candidate.failed if case_id in was_passing]
    fixes = [case_id for case_id in candidate.passed if case_id in was_failing]
    return Comparison(regressions, fixes, candidate.pass_rate - baseline.pass_rate)


def _rate(counts: tuple[int, int]) -> float:
    passed, total = counts
    return passed / total if total else 0.0


def may_ship(
    baseline: Report,
    candidate: Report,
    *,
    critical: Iterable[str] = (),
    max_tag_drop: float = 0.0,
) -> list[str]:
    """The reasons that block shipping the candidate; [] means it may ship."""
    comparison = compare(baseline, candidate)
    critical_ids = set(critical)
    reasons = [
        f"critical case '{case_id}' regressed"
        for case_id in comparison.regressions
        if case_id in critical_ids
    ]
    if comparison.delta < 0:
        reasons.append(f"pass rate dropped by {-comparison.delta * 100:.1f} points")
    for tag in sorted(set(baseline.by_tag) & set(candidate.by_tag)):
        before, after = _rate(baseline.by_tag[tag]), _rate(candidate.by_tag[tag])
        if before - after > max_tag_drop + 1e-9:
            reasons.append(f"tag '{tag}' dropped from {before:.0%} to {after:.0%}")
    return reasons
