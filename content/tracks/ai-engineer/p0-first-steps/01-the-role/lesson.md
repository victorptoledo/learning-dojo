# The AI Engineer Role and the Shape of an LLM Feature

An **AI engineer** builds products on top of models that someone else trained. You rarely train a
model from scratch. You choose one, give it the right context, connect it to tools and data, check
that it behaves, and keep it working, fast and affordable in production. It is software engineering
where one component is probabilistic.

This track assumes you can program in Python (the katas use it). If you cannot yet, take the
**Modern Python** track through Intermediate first. How to *write* prompts well (instructions,
examples, chain-of-thought) is taught in the **Prompt Engineering** track; here we build the
system around the prompt.

## Three neighbouring roles

| Role | Main question | Typical output |
| --- | --- | --- |
| Data scientist | "What does the data tell us?" | Analyses, experiments, dashboards |
| ML engineer | "How do we train and serve this model?" | Training pipelines, model weights, feature stores |
| AI engineer | "How do we build a reliable product on a foundation model?" | Features, APIs, retrieval pipelines, evals, guardrails |

The lines blur: an AI engineer may fine-tune a small model, and an ML engineer may build a RAG
service. The difference is the starting point. The AI engineer starts from a capable pre-trained
model reached through an API (or an open-weight model on their own servers) and spends most of
the time on everything around it.

## The parts of an LLM feature

Almost every feature has the same parts, and this track has a lesson for each:

1. **The model.** Chosen by quality, cost, latency, context size and where it may run. You call
   it through an API (Fundamentals).
2. **The context.** Everything the model sees in one request: instructions, the conversation,
   retrieved documents, tool results. The model knows nothing else about your users. Building
   that context is most of the job (RAG, Intermediate).
3. **The output contract.** Free text for a person, or **structured output** (JSON that matches a
   schema) for code. Code must never trust it blindly (Fundamentals).
4. **Tools.** Functions the model may ask you to run: search, look up an order, save a draft.
   A loop of model calls and tool calls is an **agent** (Intermediate).
5. **Evaluation.** A set of real inputs with expected outcomes, run on every change, so you know
   whether a new prompt or model is better or worse (Intermediate).
6. **Guardrails.** Checks before and after the model: personal data, prompt injection, what a
   tool may do without a human (Advanced).
7. **Operations.** Cost, latency, rate limits, retries, traces and user feedback (Advanced), and
   later model upgrades, fine-tuning and governance (Mastery).

```text
user ──▶ input checks ──▶ context builder ──▶ model API ──▶ output parser ──▶ output checks ──▶ user
                              │    ▲              │  ▲
                       retrieval ──┘        tools ──┘          (traces, cost and evals around all of it)
```

## What makes it different from ordinary code

- **Non-determinism.** The same input can give different outputs. You test with sets of
  examples and rates ("92% correct"), not single assertions.
- **Natural-language interfaces.** The "spec" of a model call is partly prose. A small wording
  change can move quality a lot, so prompts are code: versioned, reviewed and evaluated.
- **Failures look like successes.** A wrong answer is fluent and confident. Nothing throws. You
  find it only by measuring.
- **Cost per call.** Every request costs money in proportion to its tokens. A loop that calls the
  model ten times costs roughly ten times as much.
- **The model changes under you.** Providers release and retire models. An upgrade is a migration
  that needs your eval set.

## When not to use a model

A language model is the wrong tool when:

- the rule is exact and known ("VAT is 23%", "the field must be a valid IBAN"): write the code;
- one wrong answer is unacceptable and cannot be checked (a dose, a legal deadline);
- latency must be a few milliseconds, or the volume makes per-call cost absurd;
- a classic model (a classifier trained on your data, a search index) already does it well.

Often the best design is **mostly ordinary code with one model call** in the place where language
is genuinely fuzzy: understanding a free-text email, summarising, extracting fields.

## Start small, measure, then automate

A good first version:

1. Pick one narrow task with clear success (sort incoming emails into eight queues).
2. Collect 50–200 real examples with the right answer. This is your first **golden set**.
3. Build the simplest pipeline: one model call, structured output, validation.
4. Measure on the golden set; fix the worst failures; repeat.
5. Ship with a human in the loop (drafts, suggestions), watch real usage, and only then let it act
   alone where the measured error rate is acceptable.

## Summary

- The AI engineer builds reliable products on pre-trained models; the work is mostly context,
  contracts, tools, evaluation, guardrails and operations.
- LLM outputs are probabilistic and fail fluently: measure with example sets, not single tests.
- Use a model only where language is genuinely fuzzy; keep exact rules in code.
- Start narrow, build a golden set first, and keep a human in the loop until the numbers say
  otherwise.

## Further reading

- Anthropic, "Building effective agents" (workflows versus agents, start simple): <https://www.anthropic.com/engineering/building-effective-agents>
- Anthropic docs, "Build with Claude" overview: <https://platform.claude.com/docs/en/build-with-claude/overview>
- OpenAI platform docs, text generation guide: <https://platform.openai.com/docs/guides/text>
- Chip Huyen, *AI Engineering: Building Applications with Foundation Models* (O'Reilly, 2025), chapter 1
- Google, "Rules of Machine Learning" (rule 1: do not be afraid to launch a product without machine learning): <https://developers.google.com/machine-learning/guides/rules-of-ml>
- roadmap.sh, AI Engineer roadmap: <https://roadmap.sh/ai-engineer>
