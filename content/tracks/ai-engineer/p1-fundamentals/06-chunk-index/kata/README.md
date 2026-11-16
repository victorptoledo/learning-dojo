# Chunks and a Vector Index

Write two modules: `chunking.py` splits documents into chunks, and `vector_index.py` stores
vectors and finds the nearest ones. Sizes are counted in **words** (`text.split()`), a stand-in
for tokens.

## `chunking.py`

`Chunk(doc_id, index, text)` is given (a frozen dataclass).

- `split_paragraphs(text)`: paragraphs are separated by one or more blank lines (lines with only
  whitespace). Inside a paragraph, collapse all whitespace (line breaks included) to single
  spaces. Drop empty paragraphs. Returns a list of strings.
- `chunk_document(doc_id, text, max_words, overlap=0)`: returns the chunks of one document,
  numbered from 0:
  - **pack** consecutive paragraphs into one chunk (joined with `"\n\n"`) while the chunk has at
    most `max_words` words;
  - a paragraph **longer** than `max_words` is never packed with others: it becomes windows of
    `max_words` words, each starting `max_words - overlap` words after the previous one, the last
    window ending at the paragraph's end (no window that only repeats words of the previous one);
  - `max_words` below 1, or `overlap` below 0 or not below `max_words`, raises `ValueError`.

```python
chunk_document("doc", "a b c\n\nd e\n\nf g h i j k l", max_words=5, overlap=2)
# [Chunk("doc", 0, "a b c\n\nd e"), Chunk("doc", 1, "f g h i j"), Chunk("doc", 2, "i j k l")]
```

## `vector_index.py`

- `normalize(vector)`: the vector divided by its length, as a tuple of floats. A zero vector
  raises `ValueError`.
- `cosine(a, b)`: cosine similarity of two vectors. Different lengths or a zero vector raise
  `ValueError`.
- `VectorIndex(dimension)` stores items `(id, vector, metadata)`:
  - `add(item_id, vector, metadata=None)`: the vector must have `dimension` numbers and not be all
    zeros (`ValueError`); store it normalised. Adding an id again replaces the item. Metadata
    defaults to `{}`.
  - `remove(item_id)`: `KeyError` for an unknown id.
  - `len(index)` and `item_id in index`.
  - `search(query, k=5, where=None, min_score=None)`: returns up to `k` `Hit(id, score, metadata)`
    (given), best first, ties broken by id. `score` is the cosine similarity. `where` is a dict
    of metadata values that must all be equal; `min_score` drops hits below it. `k` below 1 and
    a query of the wrong dimension or all zeros raise `ValueError`.
- `rrf(rankings, k=60)`: reciprocal rank fusion of several ranked lists of ids. An id at
  position `rank` (1 for the first) in a list scores `1 / (k + rank)`; scores add up over the
  lists. Returns `(id, score)` pairs, best first, ties broken by id.

Run the visible tests with `uv run pytest`. There are 27 tests: 2 in `tests/test_visible.py`
and 25 hidden ones, plus ruff on each module.
