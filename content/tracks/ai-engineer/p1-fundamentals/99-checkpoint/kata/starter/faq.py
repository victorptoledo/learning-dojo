"""Semantic search over a Markdown FAQ."""

from collections.abc import Callable, Sequence
from dataclasses import dataclass
from typing import Any

from embedder import embed
from tokenizer import count_tokens  # noqa: F401 (build_context uses it)
from vector_index import VectorIndex  # noqa: F401 (FaqSearch uses it)


@dataclass(frozen=True)
class Entry:
    id: str
    section: str
    question: str
    answer: str
    tags: tuple[str, ...] = ()


@dataclass(frozen=True)
class Match:
    entry_id: str
    question: str
    score: float


class FaqFormatError(ValueError):
    def __init__(self, line: int, message: str) -> None:
        super().__init__(f"line {line}: {message}")
        self.line = line


def slugify(question: str) -> str:
    raise NotImplementedError("todo: write slugify()")


def parse_faq(markdown: str) -> list[Entry]:
    raise NotImplementedError("todo: write parse_faq()")


class FaqSearch:
    def __init__(self, entries: Sequence[Entry], embed: Callable[[str], Sequence[float]] = embed) -> None:
        raise NotImplementedError("todo: build the index")

    def entry(self, entry_id: str) -> Entry:
        raise NotImplementedError("todo: write FaqSearch.entry()")

    def search(self, query: str, k: int = 3, min_score: float = 0.5, section: str | None = None) -> list[Match]:
        raise NotImplementedError("todo: write FaqSearch.search()")

    def build_context(self, matches: Sequence[Match], budget_tokens: int) -> str:
        raise NotImplementedError("todo: write FaqSearch.build_context()")

    def answer(self, query: str, min_score: float = 0.5) -> dict[str, Any]:
        raise NotImplementedError("todo: write FaqSearch.answer()")
