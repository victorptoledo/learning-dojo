# Checkpoint 3: Grounded Answers

A help-centre assistant answers questions from retrieved documents. The documents come from
outside (product pages, reviews, forum posts), so they are **untrusted**. Write `grounded.py`:
it packs the documents into a budget, guards against injection, accepts an answer only when its
quotes are real and its links are safe, and evaluates the whole flow on a labelled set.

The dataclasses and constants are given: `Source(id, title, text, score)`,
`Prompt(messages, included, omitted, flags, tokens)`, `Quote(doc, text)`,
`Answer(text, quotes)` with the property `insufficient` (the text is exactly `INSUFFICIENT`),
`Result(status, text, problems, prompt)`, `Case(id, question, sources, expect, must_contain="", tags=())`,
`EvalReport(passed, failed, by_tag)`, and `RULES`, `ZERO_WIDTH`, `PATTERNS`, `URL`, `OUTCOMES`.

## Building the prompt

| Function | Does |
| --- | --- |
| `estimate_tokens(text)` | `ceil(len(text) / 4)` |
| `neutralise(text)` | Removes the `ZERO_WIDTH` characters and control characters (Unicode category `Cc`) except `\n` and `\t`, then `html.escape(text, quote=False)`. |
| `find_suspicious(text)` | The labels of `PATTERNS` that match the **raw** text, in the order of `PATTERNS`. |
| `build_prompt(question, sources, *, budget_tokens)` | See below. |

`build_prompt` returns two messages: a system message whose content is exactly `RULES`, and a
user message:

```text
<documents>
<document id="ID" title="TITLE">
NEUTRALISED TEXT
</document>
...one block per included document...
</documents>

<question>
NEUTRALISED QUESTION (stripped)
</question>
```

`ID` and `TITLE` are escaped with `html.escape(..., quote=True)`. Documents go in by score,
highest first (equal scores by id). A document that would take the prompt over the budget is
**omitted**, and the next ones are still tried. The cost of the prompt is
`estimate_tokens(system) + estimate_tokens(user)`, and a prompt may use the budget exactly.

- Two sources with the same id: `ValueError`. The rules and the question alone over the budget:
  `ValueError`.
- `included` and `omitted` list ids in the order they were considered. `flags` maps the id of
  every **included** document with suspicious content to its labels. Flagged documents still go
  in: flags are for logging and review.
- `tokens` is the cost of the returned prompt.

## Checking the answer

The rules ask the model for `<quote doc="ID">...</quote>` elements and then `<answer>...</answer>`.

- `parse_answer(reply)`: the **last** `<answer>` block, `html.unescape`d and stripped; every
  `<quote doc="...">` in the reply, doc and text unescaped, text stripped. No answer, or an empty
  one: `ValueError` (`"no <answer> in the reply"` for the first).
- `unapproved_links(text, allowed_domains)`: the URLs matched by `URL` whose host is not an
  allowed domain or a subdomain of one (compare with a leading dot, ignore case and a trailing
  dot on the host), each once, in order.
- `check_answer(answer, prompt, sources, *, allowed_domains)`: the problems, in this order:
  1. for each quote, `"quote cites 'ID', which was not in the prompt"` when its doc is not in
     `prompt.included`, else `"quote not found in 'ID'"` when its text does not occur in that
     source's text. Compare ignoring case, runs of whitespace and the hidden characters that
     `neutralise` removes; an empty quote is not found;
  2. `"the answer has no supporting quote"` when the answer is not `INSUFFICIENT` and has no
     quote;
  3. `"link to an unapproved domain: URL"` for each unapproved link in the answer text.

## The flow and the evaluation

`answer_question(question, sources, model, *, budget_tokens, allowed_domains=())` builds the
prompt, calls `model` **once** with a copy of the messages, and returns a `Result`:

| Status | When | `text` | `problems` |
| --- | --- | --- | --- |
| `"rejected"` | the reply does not parse, or `check_answer` found problems | `None` | the error message, or the problems |
| `"insufficient"` | the answer is `INSUFFICIENT` and has no problems | `None` | `[]` |
| `"answered"` | otherwise | the answer text | `[]` |

`evaluate(cases, model, *, budget_tokens, allowed_domains=())` first checks every case's
`expect` is one of `OUTCOMES` (else `ValueError`, before any model call). A case passes when its
status equals `expect` and, if `must_contain` is set, the text contains it (ignoring case and
spacing). A case whose prompt cannot be built (`ValueError`) fails, and the run goes on.
`by_tag` maps each tag, sorted, to `(passed, total)`.

Run the visible tests with `uv run pytest`. There are 26 tests: 2 in `tests/test_visible.py`
and 24 hidden ones, plus ruff on `grounded.py`.
