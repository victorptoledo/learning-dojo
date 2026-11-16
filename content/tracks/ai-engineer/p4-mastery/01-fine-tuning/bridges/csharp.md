---
from: csharp
status: draft
concept: "Configure before you fork"
from_way: "ML.NET models trained on your own data for classification and regression."
native_way: "Adapting a pre-trained language model: prompts and retrieval first, then fine-tuning (hosted, for example in Azure AI Foundry, or LoRA on open weights) for narrow, high-volume tasks."
sensei_tip: "For a narrow classification at huge volume, an ML.NET or small fine-tuned model can beat a large LLM on cost and latency; distil the LLM's labels into it and keep the LLM for the uncertain cases."
---

| .NET | Model customisation |
| --- | --- |
| ML.NET trainers | training small task models |
| `appsettings.json` | the prompt |
| a tuned model deployment | a fine-tuned model id |
| `TrainTestSplit` | a held-out test set |
| a fallback service | routing uncertain cases to a large model |
