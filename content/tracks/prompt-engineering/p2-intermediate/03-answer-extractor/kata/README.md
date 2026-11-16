# Answer Extractor

The prompt asked the model to reason in `<thinking>` tags and to give the final answer in
`<answer>` tags. Real responses do not always comply. Write five functions in `answers.py`.

| Function | Returns |
| --- | --- |
| `extract_tag(text, tag) -> str \| None` | The stripped content of the **last** complete `<tag>...</tag>` pair (content may span lines); `None` if there is none |
| `strip_reasoning(text) -> str` | The text without any `<thinking>...</thinking>` block, and without an unclosed `<thinking>` up to the end (a truncated answer); runs of 3 or more newlines become 2; stripped |
| `final_answer(text) -> str` | See below |
| `normalize(answer) -> str` | See below |
| `majority_vote(samples) -> tuple[str, float]` | The most common normalised final answer and its share of the samples that had one |

**`final_answer`.** If `extract_tag(text, "answer")` is a non-empty string, return it.
Otherwise look at the lines of `strip_reasoning(text)` from the **bottom** up, and return the
stripped rest of the first line that matches `Answer:` (any case, spaces allowed around the
colon and at the start). Otherwise raise `ValueError`. The fallback never looks inside
`<thinking>`: a candidate answer in the reasoning is not the final one.

**`normalize`.** Strip, `casefold`, collapse whitespace to single spaces and remove one
trailing `.`. Then, if the result is a number (`-` optional, digits with optional `,`
thousands separators in groups of three, optional decimals), remove the commas and, if there
are decimals, remove trailing zeros and a trailing `.`: `"1,234.50"` → `"1234.5"`,
`"42.0"` → `"42"`, `"42."` → `"42"`.

**`majority_vote`.** Take `normalize(final_answer(sample))` for every sample, skipping samples
that raise `ValueError`. Raise `ValueError` if none is left. The winner is the answer with the
highest count; on a tie, the one that appeared first. The share is its count divided by the
number of samples that had an answer.

```python
majority_vote(["<answer>42</answer>", "Answer: 42.0", "<answer>41</answer>", "no idea"])
# ("42", 0.6666666666666666)
```

Run the visible tests with `uv run pytest`. There are 15 tests: 2 in `tests/test_visible.py`
and 13 hidden ones, plus ruff on `answers.py`.
