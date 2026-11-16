"""Split documents into chunks for embedding and retrieval."""

from dataclasses import dataclass


@dataclass(frozen=True)
class Chunk:
    doc_id: str
    index: int
    text: str


def split_paragraphs(text: str) -> list[str]:
    raise NotImplementedError("todo: write split_paragraphs()")


def chunk_document(doc_id: str, text: str, max_words: int, overlap: int = 0) -> list[Chunk]:
    raise NotImplementedError("todo: write chunk_document()")
