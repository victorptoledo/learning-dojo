"""Run a chain of prompts with escaped hand-offs, gates, retries, a budget and a trace."""

import html
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
    return (len(text) + 3) // 4


def render_step(step: Step, value: str) -> str:
    """The step's template with {{input}} replaced by the escaped value."""
    if "{{input}}" not in step.template:
        raise ValueError(f"step '{step.name}' has no {{{{input}}}} in its template")
    return step.template.replace("{{input}}", html.escape(value, quote=False))


def run_chain(
    steps: Sequence[Step], input_text: str, model: Model, *, budget_tokens: int
) -> ChainResult:
    """Run the steps in order and return the last output with a trace."""
    if not steps:
        raise ValueError("a chain needs at least one step")
    value = input_text
    used = 0
    trace: list[StepTrace] = []
    for step in steps:
        messages = [{"role": "user", "content": render_step(step, value)}]
        problem = ""
        for attempt in range(1, step.max_attempts + 1):
            cost = sum(estimate_tokens(message["content"]) for message in messages)
            if used + cost > budget_tokens:
                raise BudgetExceeded(step.name, f"budget of {budget_tokens} tokens exceeded")
            used += cost
            answer = model(list(messages))
            used += estimate_tokens(answer)
            try:
                value = step.parse(answer)
                break
            except ValueError as error:
                problem = str(error)
                messages.append({"role": "assistant", "content": answer})
                messages.append(
                    {"role": "user", "content": f"Your answer could not be used: {problem}\nAnswer again."}
                )
        else:
            raise ChainError(step.name, problem)
        trace.append(StepTrace(step.name, attempt, value))
    return ChainResult(value, trace, used)
