"""Run a chain of prompts with escaped hand-offs, gates, retries, a budget and a trace."""

from collections.abc import Callable, Sequence
from dataclasses import dataclass

Model = Callable[[list[dict]], str]


@dataclass(frozen=True)
class Step:
    name: str
    template: str
    parse: Callable[[str], str]
    max_attempts: int = 2


@dataclass(frozen=True)
class StepTrace:
    name: str
    attempts: int
    output: str


@dataclass(frozen=True)
class ChainResult:
    output: str
    trace: list[StepTrace]
    tokens_used: int


class ChainError(Exception):
    """A step could not produce a usable answer."""

    def __init__(self, step: str, problem: str) -> None:
        super().__init__(f"step '{step}': {problem}")
        self.step = step
        self.problem = problem


class BudgetExceeded(ChainError):
    """The next call would go over the chain's token budget."""


def estimate_tokens(text: str) -> int:
    """The dojo's token estimate: ceil(len(text) / 4)."""
    raise NotImplementedError("todo: write estimate_tokens()")


def render_step(step: Step, value: str) -> str:
    """The step's template with {{input}} replaced by the escaped value."""
    raise NotImplementedError("todo: write render_step()")


def run_chain(
    steps: Sequence[Step], input_text: str, model: Model, *, budget_tokens: int
) -> ChainResult:
    """Run the steps in order and return the last output with a trace."""
    raise NotImplementedError("todo: write run_chain()")
