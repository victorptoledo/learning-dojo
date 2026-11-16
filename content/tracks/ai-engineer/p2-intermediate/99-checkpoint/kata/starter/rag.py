"""The answer path of an HR policy assistant: retrieval, prompt, verified citations."""

from collections.abc import Callable, Sequence
from dataclasses import dataclass

from embedder import embed
from llm import Model

NOT_FOUND = "I could not find this in the policies."

SYSTEM = (
    "You answer employees' questions using only the numbered sources in the message. "
    "Cite every source you use as [n]. If the sources do not contain the answer, reply exactly: "
    + NOT_FOUND
)


@dataclass(frozen=True)
class Doc:
    id: str
    title: str
    text: str
    audience: str = "all"


@dataclass(frozen=True)
class Passage:
    chunk_id: str
    doc_id: str
    title: str
    text: str
    score: float


@dataclass(frozen=True)
class Answer:
    status: str  # "answered", "not_found" or "unverified"
    text: str
    citations: tuple[int, ...]
    sources: tuple[str, ...]


def citations(text: str) -> list[int]:
    raise NotImplementedError("todo: write citations()")


class RagPipeline:
    def __init__(self, docs: Sequence[Doc], model: Model, embed: Callable[[str], Sequence[float]] = embed) -> None:
        raise NotImplementedError("todo: index the documents")

    def retrieve(self, question: str, role: str = "employee", k: int = 3, min_score: float = 0.3) -> list[Passage]:
        raise NotImplementedError("todo: write RagPipeline.retrieve()")

    def build_prompt(self, question: str, passages: Sequence[Passage], budget_tokens: int = 200) -> tuple[str, list[Passage]]:
        raise NotImplementedError("todo: write RagPipeline.build_prompt()")

    def answer(self, question: str, role: str = "employee", budget_tokens: int = 200) -> Answer:
        raise NotImplementedError("todo: write RagPipeline.answer()")


def recall_at_k(pipeline: RagPipeline, golden: Sequence[tuple[str, str, str]], k: int = 3) -> float:
    raise NotImplementedError("todo: write recall_at_k()")
