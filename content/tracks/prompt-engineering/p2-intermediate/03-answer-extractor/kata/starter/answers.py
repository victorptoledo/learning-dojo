"""Parse chain-of-thought responses: tags, final answers and self-consistency votes."""

from collections.abc import Iterable


def extract_tag(text: str, tag: str) -> str | None:
    """The stripped content of the last complete <tag>...</tag> pair, or None."""
    raise NotImplementedError("todo: write extract_tag()")


def strip_reasoning(text: str) -> str:
    """The text without <thinking> blocks, closed or truncated."""
    raise NotImplementedError("todo: write strip_reasoning()")


def final_answer(text: str) -> str:
    """The <answer> content, or the last 'Answer:' line outside the reasoning."""
    raise NotImplementedError("todo: write final_answer()")


def normalize(answer: str) -> str:
    """A canonical form of an answer, so that equal answers compare equal."""
    raise NotImplementedError("todo: write normalize()")


def majority_vote(samples: Iterable[str]) -> tuple[str, float]:
    """The most common normalised final answer and its share of the valid samples."""
    raise NotImplementedError("todo: write majority_vote()")
