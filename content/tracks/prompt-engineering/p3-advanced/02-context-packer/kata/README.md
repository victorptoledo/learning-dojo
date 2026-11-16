# Context Packer

`packer.py` defines `Document(id, title, text, score)` and
`Packed(prompt, included, truncated, omitted)`. Write four functions.

### `estimate_tokens(text) -> int`

`ceil(len(text) / 4)`.

### `render_prompt(parts, question, instructions) -> str`

`parts` is a list of `(document, text)` pairs, in prompt order; `text` is what to show for that
document. The prompt is, with `N` counting from 1:

```text
<documents>
<document index="N" id="ID" title="TITLE">
TEXT
</document>
</documents>

INSTRUCTIONS

<question>
QUESTION
</question>
```

One `<document>` block per part, each followed by a newline; with no parts the first two lines
are `<documents>` and `</documents>`. `ID` and `TITLE` are escaped with
`html.escape(..., quote=True)`; `TEXT` and the stripped `QUESTION` with `quote=False`. The
instructions are your own trusted text: stripped, not escaped.

### `pack(documents, question, *, instructions, budget_tokens, min_words=20) -> Packed`

1. If the prompt with no documents does not fit (`estimate_tokens(prompt) > budget_tokens`),
   raise `ValueError`.
2. Order the documents by score, highest first; equal scores by `id`.
3. Include each document whole while the prompt with it still fits.
4. The first document that does not fit whole is **truncated**: keep the largest number of its
   words (`text.split()`, joined by single spaces) such that the prompt with
   `KEPT_WORDS + " [truncated]"` as its text fits. If that is at least `min_words` words,
   include it and set `truncated` to its id; otherwise it is omitted.
5. Every document after that one is omitted, even if it is small (relevance order is kept).

`included` lists the ids in the prompt in order (the truncated one too), `omitted` the others
in relevance order, and `prompt` is the final rendered prompt.

### `missing_quotes(quotes, documents) -> list[str]`

The quotes that do not occur in any document's text, in order. Compare after `casefold()` and
collapsing whitespace to single spaces, on both sides. A blank quote is always missing.

Run the visible tests with `uv run pytest`. There are 14 tests: 2 in `tests/test_visible.py`
and 12 hidden ones, plus ruff on `packer.py`.
