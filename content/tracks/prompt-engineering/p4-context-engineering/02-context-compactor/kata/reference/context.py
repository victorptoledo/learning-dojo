"""Keep an agent's conversation within a token budget: clear, compact and remember."""

import html
import re
from collections.abc import Callable, Mapping, Sequence
from dataclasses import dataclass, replace

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
    return (len(text) + 3) // 4


def context_tokens(system: str, turns: Sequence[Turn]) -> int:
    """The estimate of the system prompt plus every turn's content."""
    return estimate_tokens(system) + sum(estimate_tokens(turn.content) for turn in turns)


def check_turns(turns: Sequence[Turn]) -> list[str]:
    """Problems with the shape of the history; [] means it is valid."""
    problems: list[str] = []
    open_call: str | None = None
    for index, turn in enumerate(turns):
        if turn.role not in ROLES:
            problems.append(f"turn {index}: unknown role '{turn.role}'")
            continue
        if turn.pinned and turn.role != "user":
            problems.append(f"turn {index}: only user turns can be pinned")
        if turn.role == "tool":
            if turn.call_id is None or turn.call_id != open_call:
                problems.append(f"turn {index}: tool result does not follow its call")
            open_call = None
            continue
        if open_call is not None:
            problems.append(f"turn {index - 1}: call '{open_call}' has no result")
            open_call = None
        if turn.role == "assistant" and turn.call_id is not None:
            open_call = turn.call_id
        elif turn.role == "user" and turn.call_id is not None:
            problems.append(f"turn {index}: a user turn has a call id")
    if open_call is not None:
        problems.append(f"turn {len(turns) - 1}: call '{open_call}' has no result")
    return problems


def _is_cleared(turn: Turn) -> bool:
    return turn.content.startswith("[result cleared: ")


def clear_tool_results(turns: Sequence[Turn], *, keep_last: int) -> tuple[list[Turn], list[str]]:
    """Every tool result but the last `keep_last` replaced by a placeholder, and the cleared ids."""
    results = [index for index, turn in enumerate(turns) if turn.role == "tool"]
    keep = set(results[len(results) - keep_last :]) if keep_last > 0 else set()
    new_turns: list[Turn] = []
    cleared: list[str] = []
    for index, turn in enumerate(turns):
        if turn.role == "tool" and index not in keep and not _is_cleared(turn):
            placeholder = CLEARED.format(tokens=estimate_tokens(turn.content))
            new_turns.append(replace(turn, content=placeholder))
            cleared.append(turn.call_id or "")
        else:
            new_turns.append(turn)
    return new_turns, cleared


def split_point(turns: Sequence[Turn], keep_recent: int) -> int:
    """Where the recent turns start, moved back so a tool result stays with its call."""
    start = max(0, len(turns) - keep_recent)
    while 0 < start < len(turns) and turns[start].role == "tool":
        start -= 1
    return start


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
    problems = check_turns(turns)
    if problems:
        raise ValueError("; ".join(problems))
    turns = list(turns)
    tokens = context_tokens(system, turns)
    if tokens <= budget_tokens:
        return Context(system, turns, tokens, [], 0)
    turns, cleared = clear_tool_results(turns, keep_last=keep_tool_results)
    tokens = context_tokens(system, turns)
    if tokens <= budget_tokens:
        return Context(system, turns, tokens, cleared, 0)
    start = split_point(turns, keep_recent)
    old, recent = turns[:start], turns[start:]
    pinned = [turn for turn in old if turn.pinned]
    to_summarise = [turn for turn in old if not turn.pinned]
    if not to_summarise:
        raise ContextOverflow(tokens, budget_tokens)
    summary = html.escape(summarise(list(to_summarise)).strip(), quote=False)
    turns = pinned + [Turn("user", f"<summary>\n{summary}\n</summary>")] + recent
    tokens = context_tokens(system, turns)
    if tokens > budget_tokens:
        raise ContextOverflow(tokens, budget_tokens)
    return Context(system, turns, tokens, cleared, len(to_summarise))


def apply_memory(notes: Mapping[str, str], reply: str) -> dict[str, str]:
    """A new notes dict with the reply's <memory key="..."> updates; an empty value deletes."""
    updated = dict(notes)
    for raw_key, raw_value in MEMORY.findall(reply):
        key = html.unescape(raw_key)
        if not KEY.fullmatch(key):
            raise ValueError(f"invalid memory key '{key}'")
        value = html.unescape(raw_value).strip()
        if value:
            updated[key] = value
        else:
            updated.pop(key, None)
    return updated


def render_system(base: str, notes: Mapping[str, str]) -> str:
    """The base system prompt followed by the notes, sorted by key and escaped."""
    base = base.strip()
    if not notes:
        return base
    lines = [f'<note key="{key}">{html.escape(notes[key], quote=False)}</note>' for key in sorted(notes)]
    return base + "\n\n<memory>\n" + "\n".join(lines) + "\n</memory>"
