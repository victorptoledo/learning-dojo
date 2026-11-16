# RAG: Chunking, Retrieval, Reranking and Grounded Answers

A model knows only its training data, frozen at some date, and nothing about your company.
**Retrieval-augmented generation** (RAG) fixes that at request time: find the passages that are
relevant to the question and put them in the prompt, so the model answers **from your documents**.
It is the most common architecture in AI engineering, because it is cheaper than fine-tuning,
updates as soon as the documents do, and lets you show sources.

## Two pipelines

```text
ingestion (offline, on every document change)
  documents ─▶ parse ─▶ clean ─▶ chunk ─▶ embed ─▶ index (vectors + text + metadata)

query (online, per question)
  question ─▶ rewrite? ─▶ retrieve (vector + keyword) ─▶ rerank ─▶ assemble prompt ─▶ model ─▶ check ─▶ answer + sources
```

Most quality problems are born in ingestion, and most teams look for them in the prompt.

## Ingestion

- **Parsing.** PDFs, HTML, slides and spreadsheets need real extraction: keep headings, lists and
  tables (as Markdown or one row per line with headers repeated), drop navigation and boilerplate.
  Garbage in the index means garbage in the prompt.
- **Chunking.** Chunks must be small enough to be specific and to fit several in a prompt, and
  large enough to carry a complete thought. Common strategies:
  - fixed size (for example 300–800 tokens) with **overlap** (10–20%), simple and robust;
  - **structural**: by heading, section or paragraph, merging small pieces and splitting big ones
    (what you built in the chunk kata);
  - semantic: split where the topic changes (embedding similarity between sentences).
  Prefix each chunk with context it lost, like the document title and section path ("Travel
  policy › Expenses › Meals"): this one change often improves retrieval a lot.
- **Metadata.** Store source, title, section, URL, version or date, language, and **access
  control** (team, tenant, role) with every chunk. Retrieval filters on it.
- **Updates.** Re-index changed documents, delete chunks of removed or replaced versions, and
  record the embedding model id. Stale chunks are a classic cause of confidently wrong answers.

## Retrieval

- **Query rewriting.** User questions are short and conversational ("and for contractors?"). A
  cheap model call can rewrite the question into a standalone query using the conversation, or
  generate a hypothetical answer to embed (HyDE), or split a compound question.
- **Hybrid search.** Vector search for meaning plus BM25 for exact terms, fused with RRF.
- **Filters first.** Apply permission and scope filters inside the search, never by asking the
  model to ignore documents it should not see.
- **Reranking.** Retrieve generously (top 20–50), then reorder with a **cross-encoder reranker**
  that reads the question and each passage together, and keep the best 3–8. Rerankers are slower
  per item but far more precise than embedding similarity.
- **Threshold.** If even the best passage is weak, it is better to say "not found" than to answer
  from noise.

## Assembling the prompt

```text
System: Answer the employee's question using only the sources below. Cite sources as [1], [2].
If the sources do not contain the answer, say you could not find it and suggest who to ask.

Sources:
[1] Travel policy › Meals (v2026-03): Meals are reimbursed up to 40 EUR per day ...
[2] Travel policy › Approvals (v2026-03): Trips abroad need approval from ...

Question: Can I expense a 55 EUR dinner in Madrid?
```

- **Budget the context.** Count tokens and keep the best passages that fit, leaving room for the
  answer. Order matters: put the most relevant first (or first and last).
- **Number the sources** and ask for citations. Then **check them in code**: every cited number
  must exist among the sources you sent, and an answer with no citation is suspicious.
- **Give an explicit way out.** "If the sources do not contain the answer, say so." Models
  prefer answering to admitting ignorance unless allowed and asked to.
- **Treat retrieved text as data, not instructions**: a document may contain "ignore previous
  instructions" (prompt injection, Advanced).

Many APIs have built-in support: document blocks with automatic citations (Anthropic), file
search tools (OpenAI), grounding with search (Gemini). They save work, but the ingestion and
evaluation are still yours.

## Failure modes

| Symptom | Retrieval or generation? | Typical causes |
| --- | --- | --- |
| "I could not find it" although it is documented | retrieval | bad chunking, vocabulary mismatch, k too small, threshold too strict, a filter too narrow |
| Right sources, wrong answer | generation | too much context, conflicting passages, weak instructions, a model too small |
| Answer not supported by the sources (hallucination) | generation | no "not found" path, model filling gaps from training data |
| Outdated answer | ingestion | old versions still indexed, no date metadata |
| Another customer's data in the answer | retrieval (security) | missing tenant filter |

Measure the two halves separately: **retrieval** with labelled questions and recall@k or MRR
(does the right chunk come back?), **generation** with faithfulness (is every claim supported by
the sources?) and correctness (Evaluation lesson). Fixing the prompt cannot help when the right
chunk never reached it.

## When RAG is not the answer

- The model must learn a **style or format** rather than facts: prompting or fine-tuning.
- The data is **structured** (orders, metrics): let the model call a tool or generate a SQL query
  against the database instead of embedding rows.
- The corpus is small enough to fit in the context window and is reused: put it all in, with
  prompt caching, and skip retrieval.

## Summary

- RAG = ingestion (parse, chunk with context and metadata, embed, index, keep up to date) plus
  query (rewrite, hybrid retrieve with filters, rerank, assemble, answer, check).
- Most quality problems start in parsing, chunking and stale data.
- Ask for numbered citations, verify them in code, and allow "not found".
- Diagnose failures as retrieval or generation and measure each half separately.

## Further reading

- Lewis et al., "Retrieval-Augmented Generation for Knowledge-Intensive NLP Tasks" (2020): <https://arxiv.org/abs/2005.11401>
- Gao et al., "Precise Zero-Shot Dense Retrieval without Relevance Labels" (HyDE, 2022): <https://arxiv.org/abs/2212.10496>
- Anthropic, "Introducing Contextual Retrieval" (chunk context, hybrid search, reranking): <https://www.anthropic.com/news/contextual-retrieval>
- Anthropic docs, citations: <https://platform.claude.com/docs/en/build-with-claude/citations>
- OpenAI docs, retrieval and file search: <https://platform.openai.com/docs/guides/retrieval>
- Nogueira and Cho, "Passage Re-ranking with BERT" (cross-encoder reranking, 2019): <https://arxiv.org/abs/1901.04085>
- Es et al., "RAGAS: Automated Evaluation of Retrieval Augmented Generation" (2023): <https://arxiv.org/abs/2309.15217>
