---
from: go
status: draft
concept: "Retrieve, then generate"
from_way: "A search handler over Bleve or a database that returns matches."
native_way: "An ingestion worker that chunks and embeds, and a handler that retrieves, reranks, assembles a prompt and verifies citations in the answer."
sensei_tip: "Go suits the ingestion side well: a pipeline of goroutines (parse, chunk, embed in batches, upsert) with bounded concurrency to respect the embedding API's rate limits."
---

| Go | RAG step |
| --- | --- |
| a worker pool with channels | parallel ingestion |
| pgvector with `database/sql` | vectors and filters in SQL |
| `text/template` | prompt assembly |
| `sort.SliceStable` | ordering by rerank score |
| `regexp.MustCompile` with a raw-string pattern | checking citations |
