# Hello, Prompt

Write four functions in `prompt.py`. No model is called: you write the code that builds the
text a model would receive.

| Function | Does |
| --- | --- |
| `estimate_tokens(text) -> int` | The dojo's estimate: `ceil(len(text) / 4)`; `0` for `""` |
| `render(template, values) -> str` | Replaces each `{{name}}` placeholder (spaces inside the braces allowed: `{{ name }}`) with `values[name]` |
| `build_prompt(task, *, context="", rules=(), output_format="") -> str` | Assembles the sections below |
| `fits(prompt, budget, *, reserve_for_output=0) -> bool` | `True` when the prompt's estimate plus the reserve is at most the budget |

**`render` rules.** A placeholder name starts with a letter or `_` and continues with letters,
digits or `_`. If the template uses a name that is not in `values`, raise `KeyError` with that
name. If `values` has a key the template never uses, raise `ValueError` (it is almost always a
typo). Values are inserted **literally**: a value that contains `{{other}}` stays as it is.

**`build_prompt` sections**, in this order, each as `<tag>\ncontent\n</tag>` with the content
stripped, joined by one blank line (`"\n\n"`):

1. `<context>` only if `context` is not blank;
2. `<task>`, always; a blank task raises `ValueError`;
3. `<rules>` only if there is at least one non-blank rule: one line per rule, `- ` followed by
   the stripped rule;
4. `<output_format>` only if `output_format` is not blank.

```python
build_prompt("Summarise the email.", context="Refunds take 5 days.",
             rules=["At most two sentences", " "], output_format="Plain text")
```

```text
<context>
Refunds take 5 days.
</context>

<task>
Summarise the email.
</task>

<rules>
- At most two sentences
</rules>

<output_format>
Plain text
</output_format>
```

Run the visible tests with `uv run pytest`. There are 14 tests: 2 in
`tests/test_visible.py` and 12 hidden ones, plus ruff on `prompt.py`.
