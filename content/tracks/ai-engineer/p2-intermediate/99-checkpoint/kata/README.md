# Checkpoint 2: HR Policy Assistant

Write `rag.py`, the answer path of an internal assistant that answers employees' questions from
HR policies, with sources. Given (do not edit): `llm.py` (model types and `ScriptedModel`),
`embedder.py` (a deterministic stand-in embedding model with HR concepts such as meals, travel,
holiday, salary), `vector_index.py` (a finished `VectorIndex`) and `tokenizer.py`
(`count_tokens`). `Doc(id, title, text, audience)`, `Passage`, `Answer`, `SYSTEM` and
`NOT_FOUND` are given in `rag.py`.

## `RagPipeline(docs, model, embed=embed)`

Index each paragraph of each document (paragraphs are separated by `"\n\n"`), numbered from 0
per document, with the id `f"{doc.id}#{n}"` and the metadata `{"doc_id", "title", "audience"}`.
Skip paragraphs whose embedding is all zeros.

## `retrieve(question, role="employee", k=3, min_score=0.3) -> list[Passage]`

The best `k` paragraphs with a score of at least `min_score`, as
`Passage(chunk_id, doc_id, title, text, score)`, best first (ties by chunk id). A `"manager"`
sees every document; any other role only documents with `audience == "all"`. A question with an
all-zero embedding finds nothing.

## `build_prompt(question, passages, budget_tokens=200) -> tuple[str, list[Passage]]`

The user prompt, and the passages it uses:

```text
Sources:
[1] Travel policy: Meals during business trips are reimbursed up to 40 EUR per day.
[2] Travel policy: Trips abroad need approval from your manager two weeks before departure.

Question: Can I expense a 55 EUR dinner?
```

Add the passages in order while `count_tokens` of the **whole prompt** stays within
`budget_tokens`; stop at the first one that does not fit. With no passage the prompt is
`"Sources:\n\nQuestion: ..."`.

## `citations(text) -> list[int]`

The source numbers cited in the text, `[2]` or `[1, 3]` (spaces inside the brackets are allowed), in order of first appearance, without
repeats: `citations("Yes [2]. See [1, 2] and [3].")` is `[2, 1, 3]`.

## `answer(question, role="employee", budget_tokens=200) -> Answer`

1. Retrieve and build the prompt. No passage used: return
   `Answer("not_found", NOT_FOUND, (), ())` **without calling the model**.
2. Send `Request(messages=(Message("user", prompt),), system=SYSTEM)`.
3. If the reply contains `NOT_FOUND`: `Answer("not_found", reply, (), ())`.
4. If it cites at least one source and every citation is between 1 and the number of sources
   used: `Answer("answered", reply, citations, sources)`, where `sources` are the `doc_id`s of
   the cited passages in citation order, without repeats.
5. Otherwise ask **once** more: the conversation so far, the reply as an `assistant` message and
   a `user` message, either `"Your answer cited sources that do not exist: [4], [7]. Cite only
   sources [1] to [3]."` (the invalid numbers in citation order; `[1] to [n]` with n the number
   of sources used) or, with no citation at all, `"Your answer has no citations. Cite the sources
   you used as [n]."`. Judge the second reply with steps 3 and 4; if it still fails, return
   `Answer("unverified", reply, valid_citations, their_sources)`.

## `recall_at_k(pipeline, golden, k=3) -> float`

`golden` is a list of `(question, role, expected_doc_id)`. The fraction of questions whose
expected document is among the documents of `pipeline.retrieve(question, role, k=k)`; 0.0 for
an empty list.

Run the visible tests with `uv run pytest`. There are 18 tests: 2 in `tests/test_visible.py`
and 16 hidden ones, plus ruff on each module.
