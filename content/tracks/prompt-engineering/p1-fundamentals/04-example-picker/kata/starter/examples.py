"""Select and format few-shot examples."""

from collections.abc import Iterable, Sequence
from dataclasses import dataclass


@dataclass(frozen=True)
class Example:
    input: str
    output: str


def estimate_tokens(text: str) -> int:
    """The dojo's token estimate: ceil(len(text) / 4)."""
    raise NotImplementedError("todo: write estimate_tokens()")


def format_example(example: Example) -> str:
    """One example in <example>, <input> and <output> tags."""
    raise NotImplementedError("todo: write format_example()")


def pick_examples(
    pool: Sequence[Example], query: str, *, budget_tokens: int, max_examples: int = 4
) -> list[Example]:
    """Balanced, interleaved examples within the budget, never the query itself."""
    raise NotImplementedError("todo: write pick_examples()")


def few_shot_block(examples: Sequence[Example]) -> str:
    """The examples inside <examples> tags, or "" when there are none."""
    raise NotImplementedError("todo: write few_shot_block()")


def label_counts(examples: Iterable[Example]) -> dict[str, int]:
    """Examples per label, keys in order of first appearance."""
    raise NotImplementedError("todo: write label_counts()")
