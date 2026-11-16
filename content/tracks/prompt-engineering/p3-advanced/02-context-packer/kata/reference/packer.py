"""Pack ranked documents into a long-context prompt within a token budget."""

import html
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
    return (len(text) + 3) // 4


def render_prompt(parts: Sequence[tuple[Document, str]], question: str, instructions: str) -> str:
    """Labelled, escaped documents first, then the instructions, then the question."""
    blocks = []
    for index, (document, text) in enumerate(parts, start=1):
        doc_id = html.escape(document.id, quote=True)
        title = html.escape(document.title, quote=True)
        body = html.escape(text, quote=False)
        blocks.append(f'<document index="{index}" id="{doc_id}" title="{title}">\n{body}\n</document>\n')
    return (
        "<documents>\n"
        + "".join(blocks)
        + "</documents>\n\n"
        + instructions.strip()
        + "\n\n<question>\n"
        + html.escape(question.strip(), quote=False)
        + "\n</question>"
    )


def _fits(parts, question: str, instructions: str, budget: int) -> bool:
    return estimate_tokens(render_prompt(parts, question, instructions)) <= budget


def pack(
    documents: Iterable[Document],
    question: str,
    *,
    instructions: str,
    budget_tokens: int,
    min_words: int = 20,
) -> Packed:
    """Whole documents by relevance while they fit, then one marked truncation."""
    if not _fits([], question, instructions, budget_tokens):
        raise ValueError("the instructions and the question alone do not fit the budget")
    ranked = sorted(documents, key=lambda document: (-document.score, document.id))
    parts: list[tuple[Document, str]] = []
    truncated = None
    omitted: list[str] = []
    for position, document in enumerate(ranked):
        if _fits(parts + [(document, document.text)], question, instructions, budget_tokens):
            parts.append((document, document.text))
            continue
        words = document.text.split()
        low, high = 0, len(words)
        while low < high:
            middle = (low + high + 1) // 2
            cut = " ".join(words[:middle]) + " [truncated]"
            if _fits(parts + [(document, cut)], question, instructions, budget_tokens):
                low = middle
            else:
                high = middle - 1
        if low >= min_words:
            parts.append((document, " ".join(words[:low]) + " [truncated]"))
            truncated = document.id
        else:
            omitted.append(document.id)
        omitted += [later.id for later in ranked[position + 1 :]]
        break
    prompt = render_prompt(parts, question, instructions)
    return Packed(prompt, [document.id for document, _ in parts], truncated, omitted)


def _normalise(text: str) -> str:
    return " ".join(text.casefold().split())


def missing_quotes(quotes: Iterable[str], documents: Iterable[Document]) -> list[str]:
    """The quotes that occur in no document, ignoring case and spacing."""
    texts = [_normalise(document.text) for document in documents]
    missing = []
    for quote in quotes:
        needle = _normalise(quote)
        if not needle or not any(needle in text for text in texts):
            missing.append(quote)
    return missing
