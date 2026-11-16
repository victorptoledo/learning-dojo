# Fine-Tuning Trade-offs: Prompting, RAG, LoRA and Distillation

Everything so far changed what goes **into** the model. Fine-tuning changes the model itself: you
continue training it on your examples so that its weights encode a behaviour. It is powerful and
often misused. This lesson is about deciding when it is worth it, and what it costs to own.

## The ladder: try the cheap rungs first

1. **Better prompting** (instructions, examples, output schemas: see the Prompt Engineering track).
   Minutes to change, no training, works with the newest models.
2. **RAG and tools** when the model lacks **knowledge** or current data.
3. **Fine-tuning** when the model has the knowledge but not the **behaviour**: a format, a style, a
   narrow decision, done consistently, cheaply and fast.
4. **Pre-training or continued pre-training** on a domain corpus: rarely an AI engineer's call, very
   expensive.

| Need | Best first tool |
| --- | --- |
| Answer from company documents that change monthly | RAG |
| A strict output format followed every time | structured output; fine-tuning if it still fails |
| A brand voice in thousands of generated texts | prompting with examples, then fine-tuning |
| A narrow, high-volume classification at low cost and latency | fine-tune or **distil** a small model |
| Teach the model new facts | RAG (fine-tuning teaches facts poorly and makes them hard to update) |
| Use a specialised vocabulary (medical, legal) | prompting and RAG first; fine-tuning helps with style and terminology |

Fine-tuning is **not** a good way to add knowledge: facts learned in training cannot be cited, are
hard to update, and the model may still mix them up. It **is** a good way to make a smaller model
do one job like a larger one.

## Kinds of tuning

- **Supervised fine-tuning (SFT)**: pairs of input and ideal output; the model learns to produce
  the output. The workhorse.
- **Preference tuning**: pairs of a better and a worse answer for the same input. **RLHF** trains a
  reward model and optimises against it; **DPO** (direct preference optimisation) learns from the
  pairs directly and is simpler. Used to shape tone, helpfulness and refusals.
- **Reinforcement fine-tuning** with graders: the model is rewarded by a scoring function (for
  example, does the generated code pass the tests). Offered by some providers for reasoning tasks.
- **Distillation**: a large "teacher" model labels many inputs; a small "student" is fine-tuned on
  those outputs. You trade some quality for large savings in cost and latency on a narrow task.

## Parameter-efficient fine-tuning

Updating all weights of a large model needs a lot of GPU memory. **LoRA** (low-rank adaptation)
freezes the original weights and trains small low-rank matrices added to some layers, often well
under 1% of the parameters. Consequences:

- training fits on much smaller hardware; **QLoRA** goes further by keeping the frozen base in 4-bit
  precision;
- the result is a small **adapter** file; one base model can serve many adapters (one per customer
  or task);
- quality is close to full fine-tuning for most adaptation tasks.

Hosted providers offer fine-tuning of some of their models through an API (you upload JSONL
examples, they train and host the tuned model at a price); open-weight models can be tuned with
libraries such as Hugging Face TRL and PEFT, Axolotl or Unsloth.

## Data

- **Quality beats quantity.** A few hundred to a few thousand clean, consistent, representative
  examples often suffice for a narrow behaviour. Noisy labels teach noise.
- **Cover the real distribution**, including the hard and rare cases, in the format you will use in
  production (same system prompt, same input shape).
- **Hold out a test set** and never train on it; reuse your golden set as the yardstick.
- Mind **licensing and privacy**: personal data in training examples is memorised and may resurface;
  some providers' terms restrict using their outputs to train competing models.
- For distillation, check teacher outputs where they are uncertain or where humans disagree.

## Evaluate like any other change

A tuned model is a candidate: compare it with the current prompt-based system on the same golden
set, per slice, with cost and latency. Watch for **regressions outside the task**: fine-tuning can
make a model worse at things it used to do well, or weaken safety behaviour, so include safety and
general cases in the eval.

## The cost of owning a tuned model

- Training runs (and the people to run them), repeated when the task, the labels or the base model
  change.
- Hosting: tuned hosted models are priced separately; self-hosted ones need GPUs (next lesson).
- **Lock-in to a base model**: when a better base model ships, your tuned model does not improve; you
  must re-tune and re-evaluate, while a prompt would have moved over in a day.
- Monitoring for drift as inputs change.

Often the best design is a **cascade**: a small tuned model handles the confident majority and
routes uncertain cases to a large general model.

## Summary

- Climb the ladder: prompting, then RAG and tools, then fine-tuning; fine-tune for behaviour, not
  for facts.
- SFT for input-output behaviour, preference tuning (RLHF, DPO) for style and judgement,
  distillation to make a small model do one job cheaply.
- LoRA and QLoRA make tuning cheap and produce small adapters.
- Clean, representative data and a held-out test set matter more than volume.
- Evaluate against the current system, watch for regressions, and count the cost of re-tuning and
  hosting.

## Further reading

- Hu et al., "LoRA: Low-Rank Adaptation of Large Language Models" (2021): <https://arxiv.org/abs/2106.09685>
- Dettmers et al., "QLoRA: Efficient Finetuning of Quantized LLMs" (2023): <https://arxiv.org/abs/2305.14314>
- Rafailov et al., "Direct Preference Optimization" (2023): <https://arxiv.org/abs/2305.18290>
- Ouyang et al., "Training language models to follow instructions with human feedback" (2022): <https://arxiv.org/abs/2203.02155>
- Hinton, Vinyals and Dean, "Distilling the Knowledge in a Neural Network" (2015): <https://arxiv.org/abs/1503.02531>
- OpenAI docs, model optimization and fine-tuning: <https://platform.openai.com/docs/guides/model-optimization>
- Hugging Face, PEFT documentation: <https://huggingface.co/docs/peft>
- Hugging Face, TRL documentation: <https://huggingface.co/docs/trl>
