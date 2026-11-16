# Checkpoint 1: Fundamentals

This checkpoint closes the **Fundamentals** phase. It has two parts, and you need both:

1. **The quiz** (press **Q**): ten questions over the whole phase. You need 9 right.
2. **The integration kata** (press **Enter**): a semantic search over a help-centre FAQ. You parse
   the FAQ, embed and index its questions and paragraphs, search with a threshold and a section
   filter, fit the best entries into a token budget and return a JSON-ready answer.

Do them in any order. Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| How LLMs Work | How many tokens is a page of text? What fills the context window? What does temperature change? |
| Calling Model APIs | Which stop reasons are not a success? Which errors do you retry, and how? |
| Stream Assembler | What ends an SSE event? How do tool-call arguments arrive in a stream? |
| Structured Output | JSON mode or a schema? What can a valid JSON object still get wrong? |
| Embeddings and Vector Search | Why normalise? When is exact search enough? Why add keyword search? |
| Chunks and a Vector Index | Why respect paragraphs? What is overlap for? How does RRF combine lists? |

> [!TIP]
> If a quiz question surprises you, open the lesson it comes from again before retrying. The
> explanation after each answer says which idea it tests.
