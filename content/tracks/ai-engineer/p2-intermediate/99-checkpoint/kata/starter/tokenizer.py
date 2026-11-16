"""A small, deterministic stand-in for a real tokenizer. Given: do not edit."""

import re

_PIECES = re.compile(r"\w+|[^\w\s]")
MAX_PIECE = 6


def tokenize(text: str) -> list[str]:
    """Words and punctuation marks; words longer than MAX_PIECE are cut into pieces."""
    tokens: list[str] = []
    for piece in _PIECES.findall(text):
        tokens.extend(piece[start : start + MAX_PIECE] for start in range(0, len(piece), MAX_PIECE))
    return tokens


def count_tokens(text: str) -> int:
    return len(tokenize(text))
