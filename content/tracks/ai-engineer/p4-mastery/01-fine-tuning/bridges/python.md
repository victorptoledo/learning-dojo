---
from: python
status: draft
concept: "Configure before you fork"
from_way: "scikit-learn models trained from scratch on your features."
native_way: "Starting from a pre-trained language model and adapting it with prompts, retrieval or, when justified, a LoRA fine-tune with Hugging Face PEFT and TRL."
sensei_tip: "If you know scikit-learn, the evaluation side will feel familiar: train and test splits, per-class metrics, no tuning on the test set. The training side is a few lines with TRL's SFTTrainer; the data is the real work."
---

| Python ML | LLM customisation |
| --- | --- |
| `train_test_split` | a held-out test set from the golden set |
| `fit()` on labelled data | supervised fine-tuning |
| a smaller model imitating a bigger one | distillation |
| `peft.LoraConfig` | LoRA adapters |
| `classification_report` | per-slice evaluation of the tuned model |
