# Checkpoint 4: Context Engineering

This checkpoint closes the **Context Engineering** phase. It has two parts, and you need both:

1. **The quiz** (press **Q**): eight questions over the whole phase. You need 7 right.
2. **The integration kata** (press **Enter**): a prompt optimiser. It scores a triage prompt,
   shows an optimiser model the current best prompt and a capped, clipped, escaped sample of its
   failures in a fresh context each round, rejects candidates that break the template contract,
   keeps a candidate only when it beats the best by a margin, and confirms the winner on a
   holdout set the optimiser never sees.

The kata joins the two halves of the phase. Metaprompting gives the loop its purpose, and
context engineering decides what the optimiser sees: not the growing history of every attempt,
but the smallest set of high-signal tokens for the next step.

Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| Context Engineering | What fills a long context? Why clear and compact, and in which order? What must a sub-agent's brief contain? Why does compaction cost cache hits? |
| Metaprompting | What is the template contract? Why a fresh context per round? Why cap the failures? How do holdout sets, margins and reviews stop overfitting and leakage? |
| The kata of the phase | How did the compactor keep tool calls and results together? Which turns did it never summarise? |
