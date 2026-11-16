# Checkpoint 1: Ticket Triage

A support team wants each incoming ticket classified by a model. You write the code around the
prompt in `triage.py`. The constants are given:

```python
CATEGORIES = ("billing", "bug", "account", "other")
PRIORITIES = ("low", "medium", "high")
FIELDS = ("category", "priority", "summary")
MAX_SUMMARY = 200
```

| Function | Does |
| --- | --- |
| `system_prompt() -> str` | **Your** system prompt (see below) |
| `escape_ticket(text) -> str` | `html.escape` of the stripped text with `quote=False`, so a ticket cannot close the `<ticket>` tag |
| `validate(answer) -> list[str]` | Every problem with a parsed answer, in the order below; `[]` when it is valid |
| `parse_response(text) -> dict` | Finds, parses and validates the JSON object; raises `TriageError` |
| `build_messages(ticket, examples=()) -> list[dict]` | The message list for one ticket |
| `retry_message(error) -> dict` | The user turn that asks the model to fix its answer |

**`system_prompt()`** is free text, but it must name every category, every priority and the
three fields in quotes (`"category"`, `"priority"`, `"summary"`), say that the answer is
**JSON**, mention the `<ticket>` tag where the ticket is, and contain no `{{` placeholders.

**`validate` problems**, exactly these strings and in this order:

1. not a dict: only `the answer must be a JSON object`;
2. `missing field 'NAME'` for each missing field, in `FIELDS` order;
3. `unknown field 'NAME'` for each extra key, sorted;
4. `category must be one of: billing, bug, account, other` when a category is present but
   not one of those (also when it is not a string);
5. `priority must be one of: low, medium, high`, the same way;
6. when a summary is present: `summary must be a non-empty string` if it is not a string or
   is blank, otherwise `summary must be at most 200 characters` if the stripped text is longer.

**`parse_response`** strips the text, takes everything from the first `{` to the last `}`
(this removes code fences and preambles), and parses it with `json.loads`. No `{`...`}`:
`TriageError(["no JSON object found"])`. A parse error: `TriageError` with one problem that
starts with `invalid JSON`. Validation problems: `TriageError(problems)`. On success it returns
a new dict with the keys in `FIELDS` order and the summary stripped. `TriageError` keeps its
list in `.problems`.

**`build_messages`** returns, in order: `{"role": "system", "content": system_prompt()}`; for
each `(ticket, label)` example a user turn and an assistant turn; then the user turn of the
real ticket. A user turn's content is `"<ticket>\n" + escape_ticket(text) + "\n</ticket>"`. An
assistant turn's content is `json.dumps` of the label with the keys in `FIELDS` order. A blank
ticket raises `ValueError`; an example label that fails `validate` raises `ValueError` (a
wrong example teaches the wrong thing).

**`retry_message(error)`** returns `{"role": "user", "content": ...}` with this content:

```text
Your answer could not be used:
- PROBLEM 1
- PROBLEM 2
Reply again with only the JSON object.
```

Run the visible tests with `uv run pytest`. There are 16 tests: 2 in `tests/test_visible.py`
and 14 hidden ones, plus ruff on `triage.py`.
