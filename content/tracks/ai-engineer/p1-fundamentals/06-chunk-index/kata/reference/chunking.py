"""Split documents into chunks for embedding and retrieval."""

import re
from dataclasses import dataclass


@dataclass(frozen=True)
class Chunk:
    doc_id: str
    index: int
    text: str


_BLANK_LINES = re.compile(r"\n[ \t\r\f\v]*\n")


def split_paragraphs(text: str) -> list[str]:
    paragraphs = (" ".join(part.split()) for part in _BLANK_LINES.split(text))
    return [paragraph for paragraph in paragraphs if paragraph]


def _windows(words: list[str], size: int, overlap: int) -> list[str]:
    windows = []
    for start in range(0, len(words), size - overlap):
        windows.append(" ".join(words[start : start + size]))
        if start + size >= len(words):
            break
    return windows


def chunk_document(doc_id: str, text: str, max_words: int, overlap: int = 0) -> list[Chunk]:
    if max_words < 1 or overlap < 0 or overlap >= max_words:
        raise ValueError("need max_words >= 1 and 0 <= overlap < max_words")
    texts: list[str] = []
    pending: list[str] = []
    pending_words = 0

    def flush() -> None:
        nonlocal pending, pending_words
        if pending:
            texts.append("\n\n".join(pending))
        pending, pending_words = [], 0

    for paragraph in split_paragraphs(text):
        words = paragraph.split()
        if len(words) > max_words:
            flush()
            texts.extend(_windows(words, max_words, overlap))
            continue
        if pending_words + len(words) > max_words:
            flush()
        pending.append(paragraph)
        pending_words += len(words)
    flush()
    return [Chunk(doc_id, index, chunk) for index, chunk in enumerate(texts)]
