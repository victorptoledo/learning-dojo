# Reasoning Prompts and Chain-of-Thought

A model produces one token at a time, and each token gets a fixed amount of computation. A
question that needs several steps of work ("what does this order cost after the discount,
shipping and tax?") is hard to answer in the first token. If the model writes the steps out
first, each step becomes context for the next one, and the answer at the end can use them.
That is **chain-of-thought (CoT)** prompting.

## Three ways to ask for reasoning

**Zero-shot CoT.** Ask for it: "Think through the problem step by step before you answer."
The famous phrasing is "Let's think step by step"; the research found that this sentence alone
improved arithmetic and logic results on models of the time.

**Guided CoT.** Say *which* steps: "First find the weight band in the table. Then find the
zone. Then apply the oversize surcharge if the longest side is over 120 cm. Then apply the
business discount." This works better than generic thinking when you know the procedure.

**Few-shot CoT.** Show examples with worked reasoning, then the answer. The model imitates
both the reasoning style and the format.

## Separate the reasoning from the answer

Your code needs the answer, and your user rarely needs the scratch work. Ask for both, in
separate, named places:

```text
Work out the price in <thinking> tags, step by step, using the rules in <rules>.
Then write only the final price in euros, as a number with two decimals,
in <answer> tags.
```

Then extract the `<answer>` in code, and fall back to a clearly defined alternative (or fail)
when it is missing. With JSON output, put a `reasoning` field **before** the `answer` field,
for the same reason.

Two cautions:

- **Reasoning must come first.** "Answer, then explain" gives an explanation written to
  justify an answer already chosen.
- **Reasoning is not a faithful log.** The written steps can look right while the answer was
  reached another way, or look wrong while the answer is right. Use the reasoning to debug
  prompts, not as an audit trail or a proof.

## Self-consistency

Sample the same CoT prompt several times (with a temperature above 0), extract each final
answer, and take the **majority**. Different reasoning paths make different mistakes; the
right answer tends to be the most common one. The share of the vote is also a useful
**confidence signal**: 5 of 5 agreeing is different from 2 of 5. The cost is linear: five
samples, five times the tokens.

## Reasoning models and thinking budgets

Reasoning models (and "extended thinking" modes) do chain-of-thought by themselves, often
hidden or summarised, before the visible answer. With them:

- prefer **high-level goals and constraints** over prescribed steps: "Find the cheapest valid
  plan; explain any rule you could not satisfy" rather than a numbered procedure;
- do not ask them to "think step by step" in the visible answer; it is redundant;
- **budgets** matter: you can often set an effort level or a maximum of thinking tokens, and
  thinking tokens are billed as output;
- they take longer to start answering, which matters in interactive products.

## When reasoning helps, and when it does not

| Helps | Does not help (much) |
| --- | --- |
| arithmetic and multi-step calculations | looking up a fact the model knows or the context contains |
| applying several rules in order | simple classification with clear labels |
| planning, scheduling, logic puzzles | rewriting or translating text |
| debugging and code reasoning | tasks where latency matters more than accuracy |

The only way to know for your task is to **measure**: run the prompt with and without
reasoning (and with a reasoning model) on a labelled test set, and compare accuracy, latency and
cost per correct answer.

## Reasoning with tools

Reasoning can be interleaved with actions: think, call a tool, read the result, think again.
That loop is called **ReAct** (reason and act). You will write prompts for tools in the next
lessons; the loop itself belongs to agents, in the AI Engineer track.

## Further reading

- Wei et al., "Chain-of-Thought Prompting Elicits Reasoning in Large Language Models" (2022): <https://arxiv.org/abs/2201.11903>
- Kojima et al., "Large Language Models are Zero-Shot Reasoners" (2022): <https://arxiv.org/abs/2205.11916>
- Wang et al., "Self-Consistency Improves Chain of Thought Reasoning in Language Models" (2022): <https://arxiv.org/abs/2203.11171>
- Anthropic, "Let Claude think (chain of thought prompting)": <https://docs.anthropic.com/en/docs/build-with-claude/prompt-engineering/chain-of-thought>
- OpenAI, "Reasoning best practices": <https://platform.openai.com/docs/guides/reasoning-best-practices>
- Turpin et al., "Language Models Don't Always Say What They Think" (2023), on unfaithful explanations: <https://arxiv.org/abs/2305.04388>
