"""The answer path of an HR policy assistant: retrieval, prompt, verified citations."""

import re
from collections.abc import Callable, Sequence
from dataclasses import dataclass

from embedder import embed
from llm import Message, Model, Request
from tokenizer import count_tokens
from vector_index import VectorIndex

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


_CITATION = re.compile(r"\[\s*(\d+(?:\s*,\s*\d+)*)\s*\]")


def citations(text: str) -> list[int]:
    found: list[int] = []
    for group in _CITATION.findall(text):
        for number in (int(part) for part in group.split(",")):
            if number not in found:
                found.append(number)
    return found


class RagPipeline:
    def __init__(self, docs: Sequence[Doc], model: Model, embed: Callable[[str], Sequence[float]] = embed) -> None:
        self._model = model
        self._embed = embed
        self._texts: dict[str, str] = {}
        self._index = VectorIndex(len(embed("")))
        for doc in docs:
            for number, paragraph in enumerate(doc.text.split("\n\n")):
                vector = embed(paragraph)
                if not any(vector):
                    continue
                chunk_id = f"{doc.id}#{number}"
                self._texts[chunk_id] = paragraph
                metadata = {"doc_id": doc.id, "title": doc.title, "audience": doc.audience}
                self._index.add(chunk_id, vector, metadata)

    def retrieve(self, question: str, role: str = "employee", k: int = 3, min_score: float = 0.3) -> list[Passage]:
        vector = self._embed(question)
        if not any(vector) or len(self._index) == 0:
            return []
        where = None if role == "manager" else {"audience": "all"}
        hits = self._index.search(vector, k=k, where=where, min_score=min_score)
        return [
            Passage(hit.id, hit.metadata["doc_id"], hit.metadata["title"], self._texts[hit.id], hit.score)
            for hit in hits
        ]

    def build_prompt(self, question: str, passages: Sequence[Passage], budget_tokens: int = 200) -> tuple[str, list[Passage]]:
        def render(used: Sequence[Passage]) -> str:
            sources = [f"[{n}] {p.title}: {p.text}" for n, p in enumerate(used, start=1)]
            return "\n".join(["Sources:", *sources, "", f"Question: {question}"])

        used: list[Passage] = []
        for passage in passages:
            if count_tokens(render([*used, passage])) > budget_tokens:
                break
            used.append(passage)
        return render(used), used

    def answer(self, question: str, role: str = "employee", budget_tokens: int = 200) -> Answer:
        prompt, used = self.build_prompt(question, self.retrieve(question, role), budget_tokens)
        if not used:
            return Answer("not_found", NOT_FOUND, (), ())
        messages = [Message("user", prompt)]
        text = ""
        valid: list[int] = []
        for attempt in range(2):
            text = self._model.complete(Request(tuple(messages), system=SYSTEM)).text
            if NOT_FOUND in text:
                return Answer("not_found", text, (), ())
            cited = citations(text)
            valid = [n for n in cited if 1 <= n <= len(used)]
            if cited and len(valid) == len(cited):
                return Answer("answered", text, tuple(valid), _sources(valid, used))
            if attempt == 0:
                messages += [Message("assistant", text), Message("user", _repair(cited, valid, len(used)))]
        return Answer("unverified", text, tuple(valid), _sources(valid, used))


def _sources(numbers: Sequence[int], used: Sequence[Passage]) -> tuple[str, ...]:
    doc_ids: list[str] = []
    for number in numbers:
        doc_id = used[number - 1].doc_id
        if doc_id not in doc_ids:
            doc_ids.append(doc_id)
    return tuple(doc_ids)


def _repair(cited: Sequence[int], valid: Sequence[int], count: int) -> str:
    invalid = [n for n in cited if n not in valid]
    if not cited:
        return "Your answer has no citations. Cite the sources you used as [n]."
    listed = ", ".join(f"[{n}]" for n in invalid)
    return f"Your answer cited sources that do not exist: {listed}. Cite only sources [1] to [{count}]."


def recall_at_k(pipeline: RagPipeline, golden: Sequence[tuple[str, str, str]], k: int = 3) -> float:
    if not golden:
        return 0.0
    found = 0
    for question, role, expected in golden:
        if expected in {passage.doc_id for passage in pipeline.retrieve(question, role, k=k)}:
            found += 1
    return found / len(golden)
