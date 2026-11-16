# Evaluation: Golden Sets, LLM-as-Judge and Regression Testing

"It looked good in the demo" is how LLM features fail in production. Because outputs vary and
errors are fluent, the only way to know whether a feature works, and whether a change made it
better or worse, is to **measure it on many examples**. Evaluation is to AI engineering what
tests are to ordinary software, and good teams write the eval before the prompt.

## The golden set

A **golden set** is a list of inputs with what a good output looks like:

```json
{"id": "t-017", "input": "I was charged twice for order A-1234!", "expected": {"category": "billing", "urgency": 4}, "tags": ["billing", "duplicate-charge"]}
```

- **Start from real data**: logs, tickets, questions users asked. Invented examples miss the
  messy cases. Remove personal data.
- **Label carefully.** Expected outputs come from experts or a checked process. Disagreements
  between labellers reveal ambiguous cases, which are valuable.
- **Cover slices** with tags: each category, language, length, known hard cases, past bugs. A
  2-point overall gain can hide a 20-point loss on one slice.
- **Size**: 50 good examples beat 5,000 sloppy ones to start; grow it with every bug found in
  production (each one becomes a case).
- **Keep it out of prompts**: if you tune the prompt on the same examples you test on, scores
  overfit. Keep a held-out part.

## Metrics: from cheap and exact to expensive and fuzzy

| Kind | Examples | Use for |
| --- | --- | --- |
| **Exact / code-based** | exact match after normalising, accuracy, precision and recall per class, JSON validity, regex checks, "contains these keywords", numeric tolerance | classifications, extraction, formats, anything with a right answer |
| **Retrieval** | recall@k, MRR, nDCG | the search part of RAG |
| **Reference similarity** | embedding similarity to a reference answer, ROUGE, BLEU | weak signals for free text; use with care |
| **LLM-as-judge** | a model scores the output against a rubric, or compares two outputs | tone, helpfulness, faithfulness to sources, summary quality |
| **Human review** | experts rate a sample | ground truth for the judge, high-stakes decisions |

Prefer code-based checks wherever they work: they are free, fast and exact. Split fuzzy goals
into checkable parts ("mentions the refund deadline", "cites only sources that were provided",
"no more than 3 sentences").

## LLM-as-judge

A judge model receives the input, the output (and the reference or sources) and a **rubric**,
and returns a score with a reason, as structured output:

```text
Rate the summary from 1 to 5 for faithfulness to the ticket.
5: every statement is supported by the ticket. 3: one minor unsupported detail. 1: invents facts.
Return JSON: {"reason": "...", "score": n}
```

Known biases and how to limit them:

- **Position bias** in pairwise comparisons: the first answer wins more often. Run both orders and
  count only consistent wins.
- **Verbosity bias**: longer answers score higher. Say in the rubric that length is not quality.
- **Self-preference**: a model prefers outputs from its own family. Use a different judge model
  where you can.
- **Vague scales**: "rate 1 to 10" is noisy. Use few levels with written definitions, or binary
  pass/fail per criterion.

**Calibrate the judge**: have humans rate 50–100 outputs, run the judge on the same ones, and
measure agreement. Only trust the judge for criteria where it agrees with people. Re-check when
you change the judge model or prompt.

## Regression testing

Every change to a prompt, model, retrieval setting or tool is a candidate. Run the **same golden
set** on the **baseline** (what is in production) and the **candidate**, then compare:

- overall pass rate and per-slice pass rates;
- **regressions**: cases that passed before and fail now (look at every one);
- fixes: cases that failed before and pass now;
- cost and latency per case.

Turn it into a **gate in CI**: the candidate ships only if the overall score does not drop more
than a tolerance, no critical slice regresses, and nobody has waved through unexplained
regressions. Because outputs vary, run each case more than once (or at low temperature) for
noisy metrics and do not chase differences smaller than the noise.

## Online evaluation

Offline sets cannot cover everything. After launch, collect:

- **explicit feedback** (thumbs up or down, corrections), biased towards unhappy users but useful;
- **implicit signals**: accepted suggestions, edits to drafts, retries, escalations to humans,
  abandoned sessions;
- **sampled judging** of live traffic (with privacy rules) on the same rubric as offline;
- **A/B tests** for changes whose effect only users can show.

Feed the failures back into the golden set. The set, the metrics and the gate are the asset your
team keeps as models come and go.

## Summary

- Write the eval first: a golden set from real, labelled data with tags for slices.
- Use code-based metrics wherever possible; LLM-as-judge for fuzzy criteria, with a rubric,
  structured output, bias controls and calibration against humans.
- Gate every change by comparing candidate and baseline on the same set, per slice, and inspect
  each regression.
- After launch, add online signals and turn production failures into new cases.

## Further reading

- Zheng et al., "Judging LLM-as-a-Judge with MT-Bench and Chatbot Arena" (2023): <https://arxiv.org/abs/2306.05685>
- Anthropic docs, define success criteria and build evaluations: <https://platform.claude.com/docs/en/test-and-evaluate/develop-tests>
- OpenAI docs, evals: <https://platform.openai.com/docs/guides/evals>
- Hamel Husain, "Your AI Product Needs Evals": <https://hamel.dev/blog/posts/evals/>
- Shankar et al., "Who Validates the Validators? Aligning LLM-Assisted Evaluation of LLM Outputs with Human Preferences" (2024): <https://arxiv.org/abs/2404.12272>
- Chip Huyen, *AI Engineering* (O'Reilly, 2025), chapters 3 and 4 on evaluation
