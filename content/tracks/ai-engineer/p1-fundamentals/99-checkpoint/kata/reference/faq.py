"""Semantic search over a Markdown FAQ."""

import re
from collections.abc import Callable, Sequence
from dataclasses import dataclass
from typing import Any

from embedder import embed
from tokenizer import count_tokens
from vector_index import VectorIndex


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
    return "-".join(re.findall(r"[a-z0-9]+", question.lower()))


def _paragraphs(lines: Sequence[str]) -> list[str]:
    paragraphs: list[str] = []
    current: list[str] = []
    for line in [*lines, ""]:
        if line.strip():
            current.append(line)
        elif current:
            paragraphs.append(" ".join(" ".join(current).split()))
            current = []
    return paragraphs


def parse_faq(markdown: str) -> list[Entry]:
    entries: list[Entry] = []
    seen: set[str] = set()
    section = ""
    question: str | None = None
    question_line = 0
    body: list[str] = []

    def close() -> None:
        if question is None:
            return
        lines = list(body)
        tags: tuple[str, ...] = ()
        first = next((i for i, line in enumerate(lines) if line.strip()), None)
        if first is not None and lines[first].strip().lower().startswith("tags:"):
            raw = lines[first].strip()[len("tags:") :]
            tags = tuple(tag.strip().lower() for tag in raw.split(",") if tag.strip())
            del lines[first]
        paragraphs = _paragraphs(lines)
        if not paragraphs:
            raise FaqFormatError(question_line, f"question {question!r} has no answer")
        entry_id = slugify(question)
        if entry_id in seen:
            raise FaqFormatError(question_line, f"duplicate question id {entry_id!r}")
        seen.add(entry_id)
        entries.append(Entry(entry_id, section, question, "\n\n".join(paragraphs), tags))

    for number, line in enumerate(markdown.splitlines(), start=1):
        if line.startswith("## "):
            close()
            question, question_line, body = line[3:].strip(), number, []
        elif line.startswith("# "):
            close()
            question, body = None, []
            section = line[2:].strip()
        elif question is not None:
            body.append(line)
        elif line.strip():
            raise FaqFormatError(number, "text outside of an entry")
    close()
    return entries


class FaqSearch:
    def __init__(self, entries: Sequence[Entry], embed: Callable[[str], Sequence[float]] = embed) -> None:
        self._embed = embed
        self._entries = {entry.id: entry for entry in entries}
        self._index = VectorIndex(len(embed("")))
        for entry in entries:
            texts = [entry.question, *entry.answer.split("\n\n")]
            for number, text in enumerate(texts):
                vector = embed(text)
                if any(vector):
                    metadata = {"entry": entry.id, "section": entry.section}
                    self._index.add(f"{entry.id}#{number}", vector, metadata)

    def entry(self, entry_id: str) -> Entry:
        return self._entries[entry_id]

    def search(self, query: str, k: int = 3, min_score: float = 0.5, section: str | None = None) -> list[Match]:
        vector = self._embed(query)
        if not any(vector) or len(self._index) == 0:
            return []
        where = {"section": section} if section is not None else None
        best: dict[str, float] = {}
        for hit in self._index.search(vector, k=len(self._index), where=where, min_score=min_score):
            entry_id = hit.metadata["entry"]
            best[entry_id] = max(best.get(entry_id, hit.score), hit.score)
        ranked = sorted(best.items(), key=lambda pair: (-pair[1], pair[0]))[:k]
        return [Match(entry_id, self._entries[entry_id].question, score) for entry_id, score in ranked]

    def build_context(self, matches: Sequence[Match], budget_tokens: int) -> str:
        blocks: list[str] = []
        for match in matches:
            entry = self._entries[match.entry_id]
            candidate = [*blocks, f"Q: {entry.question}\nA: {entry.answer}"]
            if count_tokens("\n\n".join(candidate)) > budget_tokens:
                break
            blocks = candidate
        return "\n\n".join(blocks)

    def answer(self, query: str, min_score: float = 0.5) -> dict[str, Any]:
        matches = self.search(query, k=3, min_score=min_score)
        if not matches:
            return {"status": "no_match", "query": query}
        best = matches[0]
        return {
            "status": "answered",
            "query": query,
            "entry": best.entry_id,
            "question": best.question,
            "answer": self._entries[best.entry_id].answer,
            "score": round(best.score, 3),
            "related": [match.entry_id for match in matches[1:]],
        }
