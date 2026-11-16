"""An exact, in-memory vector index with cosine similarity."""

from collections.abc import Mapping, Sequence
from dataclasses import dataclass, field
from typing import Any


@dataclass(frozen=True)
class Hit:
    id: str
    score: float
    metadata: Mapping[str, Any] = field(default_factory=dict)


def normalize(vector: Sequence[float]) -> tuple[float, ...]:
    raise NotImplementedError("todo: write normalize()")


def cosine(a: Sequence[float], b: Sequence[float]) -> float:
    raise NotImplementedError("todo: write cosine()")


class VectorIndex:
    def __init__(self, dimension: int) -> None:
        raise NotImplementedError("todo: write VectorIndex")

    def add(self, item_id: str, vector: Sequence[float], metadata: Mapping[str, Any] | None = None) -> None:
        raise NotImplementedError("todo: write VectorIndex.add()")

    def remove(self, item_id: str) -> None:
        raise NotImplementedError("todo: write VectorIndex.remove()")

    def __len__(self) -> int:
        raise NotImplementedError("todo: write VectorIndex.__len__()")

    def __contains__(self, item_id: object) -> bool:
        raise NotImplementedError("todo: write VectorIndex.__contains__()")

    def search(
        self,
        query: Sequence[float],
        k: int = 5,
        where: Mapping[str, Any] | None = None,
        min_score: float | None = None,
    ) -> list[Hit]:
        raise NotImplementedError("todo: write VectorIndex.search()")


def rrf(rankings: Sequence[Sequence[str]], k: int = 60) -> list[tuple[str, float]]:
    raise NotImplementedError("todo: write rrf()")
