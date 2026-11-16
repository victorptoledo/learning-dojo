---
from: go
status: draft
concept: "Configure before you fork"
from_way: "Vendoring and patching a dependency only as a last resort."
native_way: "Fine-tuning as the last resort after prompting and retrieval, with the training data and evals versioned like code."
sensei_tip: "Training happens in Python tooling, but serving and routing often happen in Go: a router that sends confident cases to the small tuned model and the rest to the large one is a few dozen lines."
---

| Go | Model customisation |
| --- | --- |
| build flags and config | the prompt |
| interfaces and plugins | tools and RAG |
| a vendored patched module | a fine-tuned model |
| `go test` on upgrade | re-evaluating after re-tuning |
| a router with a fallback | a model cascade |
