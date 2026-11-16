# Examples and Few-Shot Prompting

Instructions describe what you want; **examples show it**. A couple of well-chosen
demonstrations often fix what paragraphs of instructions could not: the exact output format,
the tone, the level of detail, and where the boundary lies between two labels. This is
**few-shot prompting**, and it was one of the first discoveries about large language models:
the GPT-3 paper was titled "Language Models are Few-Shot Learners".

## Zero, one, few

| Style | Prompt holds | Use it when |
| --- | --- | --- |
| zero-shot | instructions only | the task is common and the format simple |
| one-shot | one example | you mostly need the format |
| few-shot | usually 2 to 8 examples | the boundary between cases is subtle, or the style is specific |

Examples cost tokens on every call, so add them when they earn their place. Start zero-shot,
look at the failures, and add examples that target them.

## What examples teach

A model infers patterns from examples, including patterns you did not intend:

- **format**: field names, punctuation, length, whether there is a preamble;
- **style and tone**: the vocabulary and register of the outputs;
- **decision boundaries**: which inputs get which label, especially near the edges;
- **accidental patterns**: if every example is short, outputs get short; if every positive
  example mentions price, the model may decide price means positive.

## Choosing examples

Good example sets are:

1. **Relevant**: they look like real inputs, not toy sentences.
2. **Diverse**: different lengths, topics, phrasings and edge cases, so the model learns the
   rule and not a surface feature.
3. **Balanced**: each label appears about equally often. Models show a **majority-label bias**
   (they favour the label seen most) and a **recency bias** (they favour the label of the last
   examples), so interleave labels rather than grouping them.
4. **Correct**: a wrong label in an example is taught as faithfully as a right one.
5. **Separate from the test data**: never use an input you will evaluate on as an example;
   your scores would mean nothing.

For tasks with many kinds of input, applications often **select examples per request**: keep a
pool of labelled examples and pick the ones most similar to the current input (by keywords or
embeddings), within a token budget. That is the kata after this lesson.

## Formatting examples

Delimit examples clearly and identically, and keep them apart from the real input:

```text
<examples>
<example>
<input>The battery lasts two days and charging is fast.</input>
<output>positive</output>
</example>
<example>
<input>Great screen, but it stopped turning on after a week.</input>
<output>mixed</output>
</example>
</examples>

Classify the review in <review> as positive, negative or mixed.

<review>
{{review}}
</review>
```

Keep the format of the examples' outputs **exactly** the format you want back: if the examples
answer `positive` and the instructions ask for `{"label": "positive"}`, you have sent two
conflicting signals.

You can also give examples as a fake conversation: `user` turns with inputs and `assistant`
turns with ideal outputs before the real `user` turn. Both work; pick one and be consistent.

## When examples hurt

- **Copying**: for open-ended writing, models may reuse the examples' phrases or structure.
  Show several varied examples, or say "these show the format, not the content".
- **Overfitting to surface features**: three examples of short emails make long emails go
  wrong.
- **Reasoning models**: models that reason before answering often do well zero-shot, and
  examples can constrain them. Try zero-shot first, then add examples if they help on your
  test set.
- **Cost**: ten examples on every call of a high-volume endpoint is real money; measure whether
  examples three to ten actually help.

## Examples of reasoning

For tasks that need reasoning, an example can show the reasoning as well as the answer: an
input, then a worked explanation, then the answer. This is **few-shot chain-of-thought**, and
the reasoning-prompts lesson in Intermediate returns to it.

## Further reading

- Brown et al., "Language Models are Few-Shot Learners" (2020): <https://arxiv.org/abs/2005.14165>
- Zhao et al., "Calibrate Before Use: Improving Few-Shot Performance of Language Models" (2021), on majority-label and recency bias: <https://arxiv.org/abs/2102.09690>
- Anthropic, "Use examples (multishot prompting)": <https://docs.anthropic.com/en/docs/build-with-claude/prompt-engineering/multishot-prompting>
- OpenAI, "Prompt engineering", few-shot learning: <https://platform.openai.com/docs/guides/prompt-engineering>
- Google, "Prompt design strategies", include few-shot examples: <https://ai.google.dev/gemini-api/docs/prompting-strategies>
- Prompt Engineering Guide, "Few-Shot Prompting": <https://www.promptingguide.ai/techniques/fewshot>
