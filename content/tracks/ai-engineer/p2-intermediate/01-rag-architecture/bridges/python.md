---
from: python
status: draft
concept: "Retrieve, then generate"
from_way: "A script that greps or searches documents and prints the matches."
native_way: "An ingestion job (parse, chunk, embed, upsert) and a query function (rewrite, retrieve, rerank, prompt, call, check citations), each measurable on its own."
sensei_tip: "LlamaIndex and LangChain give you all the pieces quickly; write the pipeline once by hand (as in the checkpoint) so you know which knob to turn when quality drops."
---

| Python | RAG step |
| --- | --- |
| `pypdf`, `unstructured`, `markdownify` | parsing |
| generators over files | streaming ingestion |
| `sentence_transformers.CrossEncoder` | reranking |
| f-strings or Jinja2 | prompt assembly |
| `re.findall(r"\[(\d+)\]", answer)` | extracting citations to check |
