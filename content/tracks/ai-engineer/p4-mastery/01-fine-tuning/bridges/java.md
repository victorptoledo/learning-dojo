---
from: java
status: draft
concept: "Configure before you fork"
from_way: "Change behaviour through configuration first, and fork a library only when you must, accepting the cost of maintaining the fork."
native_way: "Change behaviour through prompts and retrieval first, and fine-tune only when you must, accepting the cost of re-tuning and hosting the result."
sensei_tip: "A fine-tuned model is a fork of the base model: it will not get upstream improvements for free. Keep the golden set and the data pipeline in your repository so re-tuning is a build, not a project."
---

| Java habit | Model customisation |
| --- | --- |
| application properties | the prompt |
| a plugin or extension point | tools and RAG |
| forking a library | fine-tuning |
| a thin adapter over a library | a LoRA adapter over a base model |
| upgrade tests on a new library version | re-evaluating after re-tuning on a new base |
