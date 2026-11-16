---
from: java
status: draft
concept: "Retrieve, then generate"
from_way: "A search endpoint backed by Elasticsearch or Lucene that returns documents for the user to read."
native_way: "The same search, but its results become the context of a model call that writes an answer and cites the sources."
sensei_tip: "If your team already runs Elasticsearch, start RAG there: it does BM25, dense vectors, filters and RRF in one query. Spring AI's QuestionAnswerAdvisor wires retrieval into a ChatClient."
---

| Java world | RAG step |
| --- | --- |
| Apache Tika, PDFBox | parsing documents |
| a batch job (Spring Batch) | ingestion and re-indexing |
| Elasticsearch query with filters | hybrid retrieval with access control |
| `Comparator` over results | reranking (with a cross-encoder model) |
| Thymeleaf template | prompt assembly with numbered sources |
| `@PreAuthorize` | permission filters applied before retrieval returns |
