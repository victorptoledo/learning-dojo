"""Pack ranked documents into a long-context prompt within a token budget."""

from collections.abc import Iterable, Sequence
from dataclasses import dataclass


@dataclass(frozen=True)
class Document:
    id: str
    title: str
    text: str
    score: float


@dataclass(frozen=True)
class Packed:
    prompt: str
    included: list[str]
    truncated: str | None
    omitted: list[str]


def estimate_tokens(text: str) -> int:
    """The dojo's token estimate: ceil(len(text) / 4)."""
    raise NotImplementedError("todo: write estimate_tokens()")


def render_prompt(parts: Sequence[tuple[Document, str]], question: str, instructions: str) -> str:
    """Labelled, escaped documents first, then the instructions, then the question."""
    raise NotImplementedError("todo: write render_prompt()")


def pack(
    documents: Iterable[Document],
    question: str,
    *,
    instructions: str,
    budget_tokens: int,
    min_words: int = 20,
) -> Packed:
    """Whole documents by relevance while they fit, then one marked truncation."""
    raise NotImplementedError("todo: write pack()")


def missing_quotes(quotes: Iterable[str], documents: Iterable[Document]) -> list[str]:
    """The quotes that occur in no document, ignoring case and spacing."""
    raise NotImplementedError("todo: write missing_quotes()")
