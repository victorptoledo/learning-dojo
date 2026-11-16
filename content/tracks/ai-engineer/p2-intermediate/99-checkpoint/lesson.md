# Checkpoint 2: Intermediate

This checkpoint closes the **Intermediate** phase. It has two parts, and you need both:

1. **The quiz** (press **Q**): ten questions over the whole phase. You need 9 right.
2. **The integration kata** (press **Enter**): an HR policy assistant. Retrieval respects who may
   read what, the prompt fits a token budget with numbered sources, the model's citations are
   checked in code (with one repair attempt), "not found" is a real answer, and recall@k
   measures the retrieval.

Do them in any order. Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| RAG | Which failures come from ingestion, retrieval or generation? Where are permissions enforced? |
| Tool Calling and Agents | Who runs a tool? How is an error returned? What stops a loop? Workflow or agent? |
| Agent Loop | How are parallel calls answered? When do you check the token budget? |
| Evaluation | What makes a good golden set? When can you trust a judge? What blocks a release? |
| Eval Harness | What is a regression? Why look at slices? |

> [!TIP]
> If a quiz question surprises you, open the lesson it comes from again before retrying. The
> explanation after each answer says which idea it tests.
