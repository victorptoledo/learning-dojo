"""Improve a prompt template with a metaprompt loop, checked on a development and a holdout set."""

import html
import re
from collections.abc import Callable, Mapping, Sequence
from dataclasses import dataclass

Model = Callable[[list[dict]], str]
Grader = Callable[[str, str], bool]

PLACEHOLDER = re.compile(r"\{\{([a-z_][a-z0-9_]*)\}\}")
METAPROMPT = (
    "You improve prompt templates. Below are the current template and examples it got wrong.\n\n"
    "<current_prompt>\n{prompt}\n</current_prompt>\n\n"
    "<failures>\n{failures}</failures>\n\n"
    "Keep every placeholder of the template exactly as written. Explain what went wrong inside "
    "<analysis> tags, then write the complete new template inside <prompt> tags."
)
FAILURE = (
    '<failure id="{id}">\n<input>{input}</input>\n<expected>{expected}</expected>\n'
    "<got>{got}</got>\n</failure>\n"
)


@dataclass(frozen=True)
class Example:
    id: str
    input: str
    expected: str


@dataclass(frozen=True)
class Failure:
    id: str
    input: str
    expected: str
    got: str


@dataclass(frozen=True)
class Score:
    rate: float
    failures: list[Failure]


@dataclass(frozen=True)
class Round:
    number: int
    status: str
    dev_rate: float | None
    note: str


@dataclass(frozen=True)
class Outcome:
    prompt: str
    dev_rate: float
    holdout_rate: float
    rounds: list[Round]
    shipped: bool
    reason: str


def placeholders(template: str) -> list[str]:
    """The names of the {{placeholders}} in the template, sorted, each once."""
    return sorted(set(PLACEHOLDER.findall(template)))


def render(template: str, values: Mapping[str, str]) -> str:
    """The template with every placeholder replaced by its escaped value."""
    for name in placeholders(template):
        if name not in values:
            raise ValueError(f"no value for {{{{{name}}}}}")
    return PLACEHOLDER.sub(lambda match: html.escape(values[match.group(1)], quote=False), template)


def score(template: str, examples: Sequence[Example], model: Model, grader: Grader) -> Score:
    """Run the template on every example and grade the stripped replies."""
    if not examples:
        raise ValueError("no examples to score")
    failures: list[Failure] = []
    for example in examples:
        prompt = render(template, {"input": example.input})
        got = model([{"role": "user", "content": prompt}]).strip()
        if not grader(got, example.expected):
            failures.append(Failure(example.id, example.input, example.expected, got))
    return Score((len(examples) - len(failures)) / len(examples), failures)


def _field(text: str, max_chars: int) -> str:
    if len(text) > max_chars:
        text = text[:max_chars] + " [truncated]"
    return html.escape(text, quote=False)


def render_metaprompt(
    template: str, failures: Sequence[Failure], *, max_failures: int, max_chars: int
) -> str:
    """The optimiser's prompt: the escaped template and a capped, clipped sample of failures."""
    if not failures:
        raise ValueError("no failures to show")
    chosen = sorted(failures, key=lambda failure: failure.id)[:max_failures]
    blocks = "".join(
        FAILURE.format(
            id=html.escape(failure.id, quote=True),
            input=_field(failure.input, max_chars),
            expected=_field(failure.expected, max_chars),
            got=_field(failure.got, max_chars),
        )
        for failure in chosen
    )
    return METAPROMPT.format(prompt=html.escape(template, quote=False), failures=blocks)


def extract_candidate(reply: str, required: Sequence[str]) -> str:
    """The last <prompt> block, unescaped and stripped, with exactly the required placeholders."""
    blocks = re.findall(r"<prompt>(.*?)</prompt>", reply, re.DOTALL)
    if not blocks:
        raise ValueError("no <prompt> in the reply")
    candidate = html.unescape(blocks[-1]).strip()
    if not candidate:
        raise ValueError("the candidate is empty")
    found = set(placeholders(candidate))
    problems = [f"missing {{{{{name}}}}}" for name in sorted(set(required) - found)]
    problems += [f"unknown placeholder {{{{{name}}}}}" for name in sorted(found - set(required))]
    if problems:
        raise ValueError("; ".join(problems))
    return candidate


def optimise(
    seed: str,
    *,
    dev: Sequence[Example],
    holdout: Sequence[Example],
    model: Model,
    optimiser: Model,
    grader: Grader,
    rounds: int,
    min_gain: float = 0.0,
    max_failures: int = 5,
    max_chars: int = 300,
) -> Outcome:
    """Improve the seed on the dev set, then keep the result only if it holds on the holdout set."""
    if placeholders(seed) != ["input"]:
        raise ValueError("the seed must use exactly one placeholder, {{input}}")
    holdout_ids = {example.id for example in holdout}
    for example in dev:
        if example.id in holdout_ids:
            raise ValueError(f"example '{example.id}' is in both sets")
    seed_score = score(seed, dev, model, grader)
    best, best_score = seed, seed_score
    log: list[Round] = []
    for number in range(1, rounds + 1):
        if not best_score.failures:
            break
        metaprompt = render_metaprompt(best, best_score.failures, max_failures=max_failures, max_chars=max_chars)
        reply = optimiser([{"role": "user", "content": metaprompt}])
        try:
            candidate = extract_candidate(reply, ["input"])
        except ValueError as error:
            log.append(Round(number, "invalid", None, str(error)))
            continue
        if candidate == best:
            log.append(Round(number, "invalid", None, "the candidate is unchanged"))
            continue
        candidate_score = score(candidate, dev, model, grader)
        if candidate_score.rate > best_score.rate + min_gain + 1e-9:
            log.append(Round(number, "accepted", candidate_score.rate, ""))
            best, best_score = candidate, candidate_score
        else:
            log.append(Round(number, "rejected", candidate_score.rate, f"no gain over {best_score.rate:.0%}"))
    seed_holdout = score(seed, holdout, model, grader).rate
    if best == seed:
        reason = "nothing to improve" if not seed_score.failures else "no candidate beat the seed"
        return Outcome(seed, seed_score.rate, seed_holdout, log, False, reason)
    best_holdout = score(best, holdout, model, grader).rate
    if best_holdout < seed_holdout:
        reason = f"overfit: holdout fell from {seed_holdout:.0%} to {best_holdout:.0%}"
        return Outcome(seed, seed_score.rate, seed_holdout, log, False, reason)
    reason = (
        f"dev {seed_score.rate:.0%} -> {best_score.rate:.0%}, "
        f"holdout {seed_holdout:.0%} -> {best_holdout:.0%}"
    )
    return Outcome(best, best_score.rate, best_holdout, log, True, reason)
