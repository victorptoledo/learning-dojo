# Example Picker

`examples.py` already defines `Example(input, output)`, a frozen dataclass where `output` is
the label. Write five functions.

| Function | Returns |
| --- | --- |
| `estimate_tokens(text) -> int` | `ceil(len(text) / 4)` |
| `format_example(example) -> str` | `<example>\n<input>INPUT</input>\n<output>OUTPUT</output>\n</example>`, with input and output stripped |
| `pick_examples(pool, query, *, budget_tokens, max_examples=4) -> list[Example]` | The selection described below, in the order picked |
| `few_shot_block(examples) -> str` | `<examples>\n` + the formatted examples joined by `\n` + `\n</examples>`; `""` for no examples |
| `label_counts(examples) -> dict[str, int]` | How many examples have each label, keys in order of first appearance |

**How `pick_examples` selects.** Two inputs are *the same* when they are equal after
`casefold()` and collapsing all whitespace to single spaces.

1. Drop every example whose input is the same as `query` (the input being classified must
   never appear as an example), and every later example whose input is the same as an earlier
   one (the first one wins).
2. Group the remaining examples by label, labels in the order they first appear.
3. Go round robin: in each round, visit the labels in that order and take the **next** example
   of each label. The cost of an example is `estimate_tokens(format_example(example))`. If it
   fits in what is left of `budget_tokens`, pick it; otherwise skip it (that label tries its
   next example in the next round).
4. Stop when `max_examples` are picked or every example has been tried.

```python
pool = [Example("good", "pos"), Example("bad", "neg"), Example("great", "pos"),
        Example("awful", "neg"), Example("fine", "pos")]
pick_examples(pool, "GREAT", budget_tokens=1000, max_examples=3)
# [Example('good', 'pos'), Example('bad', 'neg'), Example('fine', 'pos')]
```

Run the visible tests with `uv run pytest`. There are 14 tests: 2 in
`tests/test_visible.py` and 12 hidden ones, plus ruff on `examples.py`.
