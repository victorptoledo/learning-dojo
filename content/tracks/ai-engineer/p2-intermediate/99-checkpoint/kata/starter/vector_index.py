"""An exact, in-memory vector index with cosine similarity. Given: do not edit."""

import math
from collections.abc import Mapping, Sequence
from dataclasses import dataclass, field
from typing import Any


@dataclass(frozen=True)
class Hit:
    id: str
    score: float
    metadata: Mapping[str, Any] = field(default_factory=dict)


def normalize(vector: Sequence[float]) -> tuple[float, ...]:
    length = math.sqrt(sum(x * x for x in vector))
    if length == 0:
        raise ValueError("cannot normalise a zero vector")
    return tuple(x / length for x in vector)


class VectorIndex:
    def __init__(self, dimension: int) -> None:
        self.dimension = dimension
        self._items: dict[str, tuple[tuple[float, ...], dict[str, Any]]] = {}

    def _unit(self, vector: Sequence[float]) -> tuple[float, ...]:
        if len(vector) != self.dimension:
            raise ValueError(f"expected {self.dimension} numbers, got {len(vector)}")
        return normalize(vector)

    def add(self, item_id: str, vector: Sequence[float], metadata: Mapping[str, Any] | None = None) -> None:
        self._items[item_id] = (self._unit(vector), dict(metadata or {}))

    def __len__(self) -> int:
        return len(self._items)

    def __contains__(self, item_id: object) -> bool:
        return item_id in self._items

    def search(
        self,
        query: Sequence[float],
        k: int = 5,
        where: Mapping[str, Any] | None = None,
        min_score: float | None = None,
    ) -> list[Hit]:
        if k < 1:
            raise ValueError("k must be at least 1")
        unit = self._unit(query)
        hits = []
        for item_id, (vector, metadata) in self._items.items():
            if where and any(metadata.get(key) != value for key, value in where.items()):
                continue
            score = sum(x * y for x, y in zip(unit, vector, strict=True))
            if min_score is None or score >= min_score:
                hits.append(Hit(item_id, score, metadata))
        hits.sort(key=lambda hit: (-hit.score, hit.id))
        return hits[:k]
