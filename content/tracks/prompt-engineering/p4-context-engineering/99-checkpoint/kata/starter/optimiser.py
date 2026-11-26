"""Improve a prompt template with a metaprompt loop, checked on a development and a holdout set."""

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
    raise NotImplementedError("todo: write placeholders()")


def render(template: str, values: Mapping[str, str]) -> str:
    """The template with every placeholder replaced by its escaped value."""
    raise NotImplementedError("todo: write render()")


def score(template: str, examples: Sequence[Example], model: Model, grader: Grader) -> Score:
    """Run the template on every example and grade the stripped replies."""
    raise NotImplementedError("todo: write score()")


def render_metaprompt(
    template: str, failures: Sequence[Failure], *, max_failures: int, max_chars: int
) -> str:
    """The optimiser's prompt: the escaped template and a capped, clipped sample of failures."""
    raise NotImplementedError("todo: write render_metaprompt()")


def extract_candidate(reply: str, required: Sequence[str]) -> str:
    """The last <prompt> block, unescaped and stripped, with exactly the required placeholders."""
    raise NotImplementedError("todo: write extract_candidate()")


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
    raise NotImplementedError("todo: write optimise()")
