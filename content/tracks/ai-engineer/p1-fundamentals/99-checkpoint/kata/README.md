# Checkpoint 1: Semantic FAQ Search

A help centre keeps its FAQ in one Markdown file. Write `faq.py`: parse the file, index it with
embeddings, find the entries that answer a question, and build the context and the answer a chat
assistant would use.

Given (do not edit): `embedder.py` with `embed(text)`, a tiny deterministic stand-in for an
embedding model whose ten dimensions are concepts (refund, password, shipping, ...); a text's
vector counts its words of each concept, so `embed("I want my money back")` is not zero only in
the refund dimension. `vector_index.py` is a finished `VectorIndex` like the one you wrote.
`tokenizer.py` has `count_tokens`. `faq.md` is a sample to try things with.

## The FAQ format

```markdown
# Orders

## How long does delivery take?
tags: shipping, delivery

Most parcels are delivered within 3 to 5 days.

International delivery takes up to 2 weeks because of customs.
```

- `# Name` starts a section; entries before any section have the section `""`.
- `## Question` starts an entry.
- The first non-empty line of an entry may be `tags: a, b`: tags are stripped, lower-cased, and
  empty ones dropped (no line: no tags).
- The other lines are the answer: paragraphs separated by blank lines, whitespace inside a
  paragraph collapsed to single spaces, joined with `"\n\n"`.

## Functions

- `slugify(question)`: the lower-case runs of letters and digits joined with `-`:
  `"How long does delivery take?"` becomes `"how-long-does-delivery-take"`.
- `parse_faq(markdown) -> list[Entry]`: `Entry(id, section, question, answer, tags)` is given;
  `id` is the slug, `tags` a tuple. Raise `FaqFormatError(line, message)` (given, a
  `ValueError`) with the 1-based line number for: a non-blank line before the first `##` that is
  not a `#` heading; an entry with no answer (the line of its `##`); a second entry with the same
  id (the line of the second `##`).
- `FaqSearch(entries)`: indexes each entry's question and each answer paragraph, with the
  metadata `{"entry": id, "section": section}`. Texts whose embedding is all zeros cannot be
  indexed: skip them. `entry(entry_id)` returns an `Entry` (`KeyError` if unknown).
- `search(query, k=3, min_score=0.5, section=None) -> list[Match]`: the entries (not chunks)
  whose best chunk scores at least `min_score`, best first (ties by entry id), at most `k`.
  `Match(entry_id, question, score)` is given; `score` is the best chunk's cosine similarity.
  `section` keeps only that section. A query with an all-zero embedding finds nothing.
- `build_context(matches, budget_tokens) -> str`: one block per match, in order,
  `"Q: {question}\nA: {answer}"`, joined with `"\n\n"`, as many as fit in `budget_tokens`
  (`count_tokens` of the joined text). Stop at the first block that does not fit; `""` if none.
- `answer(query, min_score=0.5) -> dict`: a JSON-ready result. With matches (`k=3`):
  `{"status": "answered", "query": ..., "entry": best id, "question": ..., "answer": the best
  entry's answer, "score": the score rounded to 3 places, "related": the other matches' ids}`.
  Without: `{"status": "no_match", "query": ...}`.

Run the visible tests with `uv run pytest`. There are 21 tests: 2 in `tests/test_visible.py`
and 19 hidden ones, plus ruff on each module.
