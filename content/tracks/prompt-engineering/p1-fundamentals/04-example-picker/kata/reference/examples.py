"""Select and format few-shot examples."""

from collections.abc import Iterable, Sequence
from dataclasses import dataclass


@dataclass(frozen=True)
class Example:
    input: str
    output: str


def estimate_tokens(text: str) -> int:
    """The dojo's token estimate: ceil(len(text) / 4)."""
    return (len(text) + 3) // 4


def format_example(example: Example) -> str:
    """One example in <example>, <input> and <output> tags."""
    return (
        f"<example>\n<input>{example.input.strip()}</input>\n"
        f"<output>{example.output.strip()}</output>\n</example>"
    )


def _normalise(text: str) -> str:
    return " ".join(text.casefold().split())


def pick_examples(
    pool: Sequence[Example], query: str, *, budget_tokens: int, max_examples: int = 4
) -> list[Example]:
    """Balanced, interleaved examples within the budget, never the query itself."""
    seen = {_normalise(query)}
    groups: dict[str, list[Example]] = {}
    for example in pool:
        key = _normalise(example.input)
        if key in seen:
            continue
        seen.add(key)
        groups.setdefault(example.output, []).append(example)

    queues = [list(group) for group in groups.values()]
    picked: list[Example] = []
    remaining = budget_tokens
    while len(picked) < max_examples and any(queues):
        for queue in queues:
            if not queue or len(picked) >= max_examples:
                continue
            example = queue.pop(0)
            cost = estimate_tokens(format_example(example))
            if cost <= remaining:
                picked.append(example)
                remaining -= cost
    return picked


def few_shot_block(examples: Sequence[Example]) -> str:
    """The examples inside <examples> tags, or "" when there are none."""
    if not examples:
        return ""
    body = "\n".join(format_example(example) for example in examples)
    return f"<examples>\n{body}\n</examples>"


def label_counts(examples: Iterable[Example]) -> dict[str, int]:
    """Examples per label, keys in order of first appearance."""
    counts: dict[str, int] = {}
    for example in examples:
        counts[example.output] = counts.get(example.output, 0) + 1
    return counts
