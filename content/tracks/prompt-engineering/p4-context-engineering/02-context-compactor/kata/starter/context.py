"""Keep an agent's conversation within a token budget: clear, compact and remember."""

import re
from collections.abc import Callable, Mapping, Sequence
from dataclasses import dataclass

CLEARED = "[result cleared: {tokens} tokens]"
ROLES = ("user", "assistant", "tool")
KEY = re.compile(r"[a-z0-9]+(?:-[a-z0-9]+)*")
MEMORY = re.compile(r'<memory key="([^"]*)">(.*?)</memory>', re.DOTALL)


@dataclass(frozen=True)
class Turn:
    role: str
    content: str
    call_id: str | None = None
    pinned: bool = False


@dataclass(frozen=True)
class Context:
    system: str
    turns: list[Turn]
    tokens: int
    cleared: list[str]
    summarised: int


class ContextOverflow(Exception):
    """Even after clearing and compacting, the context does not fit."""

    def __init__(self, tokens: int, budget: int) -> None:
        super().__init__(f"context needs {tokens} tokens, budget is {budget}")
        self.tokens = tokens
        self.budget = budget


def estimate_tokens(text: str) -> int:
    """The dojo's token estimate: ceil(len(text) / 4)."""
    raise NotImplementedError("todo: write estimate_tokens()")


def context_tokens(system: str, turns: Sequence[Turn]) -> int:
    """The estimate of the system prompt plus every turn's content."""
    raise NotImplementedError("todo: write context_tokens()")


def check_turns(turns: Sequence[Turn]) -> list[str]:
    """Problems with the shape of the history; [] means it is valid."""
    raise NotImplementedError("todo: write check_turns()")


def clear_tool_results(turns: Sequence[Turn], *, keep_last: int) -> tuple[list[Turn], list[str]]:
    """Every tool result but the last `keep_last` replaced by a placeholder, and the cleared ids."""
    raise NotImplementedError("todo: write clear_tool_results()")


def split_point(turns: Sequence[Turn], keep_recent: int) -> int:
    """Where the recent turns start, moved back so a tool result stays with its call."""
    raise NotImplementedError("todo: write split_point()")


def compact(
    system: str,
    turns: Sequence[Turn],
    *,
    budget_tokens: int,
    keep_recent: int,
    keep_tool_results: int,
    summarise: Callable[[list[Turn]], str],
) -> Context:
    """Fit the conversation in the budget: as it is, then cleared, then compacted."""
    raise NotImplementedError("todo: write compact()")


def apply_memory(notes: Mapping[str, str], reply: str) -> dict[str, str]:
    """A new notes dict with the reply's <memory key="..."> updates; an empty value deletes."""
    raise NotImplementedError("todo: write apply_memory()")


def render_system(base: str, notes: Mapping[str, str]) -> str:
    """The base system prompt followed by the notes, sorted by key and escaped."""
    raise NotImplementedError("todo: write render_system()")
