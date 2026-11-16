---
from: javascript
status: draft
concept: "Configure before you fork"
from_way: "Theming and configuring a component library before forking it."
native_way: "Shaping a model with prompts and retrieval before fine-tuning it, and calling a tuned model by its id like any other model."
sensei_tip: "From the application's point of view a hosted fine-tuned model is just another model id; keep it behind the same client and feature flag so you can switch back to the base model instantly."
---

| JavaScript | Model customisation |
| --- | --- |
| theme variables | the prompt |
| plugins | tools and RAG |
| a fork of the library | a fine-tuned model |
| a feature flag | switching between base and tuned model |
| visual regression tests | re-running the golden set |
