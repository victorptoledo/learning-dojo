---
from: javascript
status: draft
concept: "Retrieve, then generate"
from_way: "A site search (Algolia, Lunr) that lists matching pages."
native_way: "The search results are packed into a prompt, the model writes an answer with [n] citations, and the UI shows the answer with links to the sources."
sensei_tip: "Show citations as links next to the answer and let users open the source: it builds trust and makes wrong answers visible quickly."
---

| JavaScript | RAG step |
| --- | --- |
| a crawler or CMS webhook | ingestion and updates |
| Algolia, Lunr | the keyword side |
| a vector DB client | the vector side |
| template literals | prompt assembly |
| rendering `[1]` as a link | showing verified citations |
