# Context Compactor

An agent's history is a list of `Turn(role, content, call_id=None, pinned=False)`:

- `"user"` turns come from the user (or from your code, such as a summary);
- an `"assistant"` turn with a `call_id` is a **tool call**, and the very next turn must be the
  `"tool"` turn with the **same** `call_id`, holding the result;
- `pinned=True` marks a user turn that must never be summarised (the original task).

Write `context.py`. `Turn`, `Context(system, turns, tokens, cleared, summarised)`,
`ContextOverflow(tokens, budget)` and the constants are given. Never change the lists or turns
you receive: return new ones (`dataclasses.replace` helps, since turns are frozen).

## Tokens and shape

| Function | Does |
| --- | --- |
| `estimate_tokens(text)` | `ceil(len(text) / 4)` |
| `context_tokens(system, turns)` | `estimate_tokens(system)` plus `estimate_tokens(turn.content)` for every turn |
| `check_turns(turns)` | The problems, in turn order; `[]` means valid. See below. |

`check_turns` walks the turns once and reports, with `N` the turn's index:

- `"turn N: unknown role 'ROLE'"` for a role not in `ROLES` (skip the other checks for it);
- `"turn N: only user turns can be pinned"`;
- `"turn N: tool result does not follow its call"` for a tool turn that does not come right
  after an assistant call with the same `call_id`;
- `"turn N: call 'ID' has no result"` for an assistant call whose next turn is not a tool turn
  (or that is the last turn); `N` is the index of the call;
- `"turn N: a user turn has a call id"`.

## Clearing and compacting

`clear_tool_results(turns, *, keep_last)` returns `(new_turns, cleared_ids)`. Every tool turn
except the last `keep_last` tool turns gets the content `CLEARED.format(tokens=T)`, where `T` is
the estimate of its old content; the call id and the role stay. A turn that is already a
placeholder (its content starts with `"[result cleared: "`) is left alone and not reported.
`cleared_ids` lists the call ids of the turns cleared now, in order.

`split_point(turns, keep_recent)` is the index where the recent turns start:
`len(turns) - keep_recent` (at least 0), moved back while it points at a tool turn, so a result
is never separated from its call.

`compact(system, turns, *, budget_tokens, keep_recent, keep_tool_results, summarise)`:

1. If `check_turns` finds problems, raise `ValueError` (with the problems joined by `"; "`)
   before doing anything else.
2. If the context fits (`context_tokens <= budget_tokens`), return it unchanged.
3. Otherwise clear the tool results (`keep_last=keep_tool_results`). If it fits now, return it.
4. Otherwise split at `split_point(turns, keep_recent)` on the **cleared** turns. Call
   `summarise` once with the old turns that are **not** pinned. The new turns are the pinned
   old turns, then `Turn("user", "<summary>\nSUMMARY\n</summary>")` with the summary stripped
   and escaped (`html.escape(..., quote=False)`), then the recent turns.
5. If there is nothing to summarise, or the result still does not fit, raise
   `ContextOverflow(tokens, budget_tokens)` with the tokens of the best context you had.

The `Context` holds the system prompt, the final turns, their tokens, the cleared ids and the
number of turns that went into the summary.

## Notes outside the window

- `apply_memory(notes, reply)` returns a **new** dict: for each `<memory key="KEY">VALUE</memory>`
  in the reply, in order, the unescaped, stripped value is stored under the unescaped key, and an
  empty value deletes the key. A key that does not fully match `KEY` raises `ValueError`.
- `render_system(base, notes)` is `base.strip()`, and if there are notes, then a blank line,
  `<memory>`, one `<note key="KEY">VALUE</note>` line per note sorted by key with the value
  escaped (`quote=False`), and `</memory>`.

Run the visible tests with `uv run pytest`. There are 20 tests: 2 in `tests/test_visible.py`
and 18 hidden ones, plus ruff on `context.py`.
