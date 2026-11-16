# How Language Models Read a Prompt

Prompt engineering is the craft of writing the text you send to a large language model (LLM)
so that it does the job you want, reliably and at a sensible cost. To write good prompts you
need a correct mental model of what happens to that text. This lesson gives you that model; the
rest of the track builds on it.

## Text becomes tokens

A model never sees characters or words. A **tokenizer** first splits the text into **tokens**:
frequent words are often one token, rarer words are split into pieces, and spaces and
punctuation are part of tokens too.

```text
"Prompt engineering is fun!"  ->  ["Prompt", " engineering", " is", " fun", "!"]
"tokenization"                ->  ["token", "ization"]
"Schadenfreude"               ->  ["Sch", "aden", "fre", "ude"]
```

Every provider has its own tokenizer, so the same text has a different token count on
different models. A rough rule for English is **about four characters per token** (about
three quarters of a word). Code, numbers, non-English text and unusual spellings use more
tokens per character. The katas of this track use exactly that rule,
`ceil(len(text) / 4)`, as their estimator; in real code, use the provider's token-counting
endpoint or tokenizer library.

Tokens matter for three reasons:

- **Cost.** You pay per input token and per output token (output usually costs several
  times more).
- **Latency.** Output is produced one token at a time, so long answers are slow.
- **Limits.** Everything must fit in the context window.

## One token at a time

A model is a function that takes a sequence of tokens and returns a **probability for every
possible next token**. To produce an answer, the system picks one token, appends it to the
sequence and runs the model again, until it emits a stop token or reaches the output limit.
This is **autoregressive generation**.

Three consequences follow, and they explain most prompting advice:

1. **Everything conditions everything.** Each new token depends on all the tokens before it:
   your instructions, the examples, the documents and the model's own previous words. Change
   one word in the prompt and the probabilities of every later token shift.
2. **The model continues text; it does not look things up.** It has no database of facts. It
   produces the continuation that is most plausible given its training and the context. When
   the facts are not in the context, a plausible but wrong answer (a **hallucination**) is a
   natural outcome, not a malfunction.
3. **Earlier output steers later output.** If the model starts with "Sure! Here is the JSON:",
   what follows is shaped by that start. If it reasons first and answers afterwards, the answer
   can use the reasoning; if it answers first, any explanation afterwards is a justification.

## Sampling and temperature

Given the probabilities, the system **samples** the next token. Settings control how:

| Setting | Effect |
| --- | --- |
| `temperature` | Sharpens (low) or flattens (high) the distribution. Near 0: almost always the most likely token. Higher: more varied, more creative, more mistakes. |
| `top_p` | Samples only from the smallest set of tokens whose probabilities add up to `p`. |
| `max_tokens` (or similar) | Hard limit on output length; the answer is cut off when it is reached. |
| stop sequences | Strings that end generation as soon as they are produced. |

Even at temperature 0 the output is not guaranteed to be byte-for-byte identical across runs
(batching, hardware and model updates all play a part). Some reasoning models do not let you
set the temperature at all. **Treat model output as a distribution, not a value**: test each
prompt on several inputs and several runs.

## The context window

The **context window** is the maximum number of tokens the model can take into account at
once: the system prompt, the whole conversation so far, any documents and tool results, **and
the answer it is writing**. Windows today range from tens of thousands to over a million
tokens, but bigger is not free:

- you pay for every input token on every call (chat APIs are stateless: the client sends the
  whole conversation each time);
- quality on details can drop as the context grows, especially for facts buried in the middle
  (you will study this in the long-context lesson);
- the window is a **budget**: reserve room for the output before you fill it with input.

The model knows only two things: what it learned in training (up to its **knowledge
cutoff**) and what is in the context window now. It has no memory of other conversations
unless your application puts that text back into the context.

## Chat models and messages

Modern APIs take a list of **messages** instead of one string. Each message has a role:

```json
[
  {"role": "system", "content": "You are a support assistant for an online bookshop."},
  {"role": "user", "content": "Where is my order 1234?"}
]
```

Under the hood, the messages are turned into one token sequence with special marker tokens
between the roles. The model was trained to treat the system part as the operator's
instructions and to continue with an `assistant` turn. You will study roles in Fundamentals.

## What changes with reasoning models

Some models (often called **reasoning models**) generate a hidden or summarised chain of
reasoning tokens before the visible answer. They still work token by token; they just spend
tokens thinking first. They need less "think step by step" coaching, they bill the reasoning
tokens as output, and they respond best to clear goals and constraints rather than
micro-managed steps. The reasoning lesson covers when to use which.

## Why prompt engineering is engineering

Because outputs are probabilistic and depend on every token of the context, a prompt is not a
magic phrase. It is an **interface** to a component whose behaviour you can only measure. The
discipline is the same as for any other component:

- write down what a good result looks like (the **success criteria**);
- build a set of test inputs and check the outputs against those criteria;
- change one thing at a time and keep the version that measurably works better;
- treat the prompt as code: version it, review it, and test it again when the model changes.

## How the katas of this track work

The dojo runs offline and has no built-in AI, so no kata calls a model. Instead, the katas are
the code that surrounds every real prompt: Python functions that **build** prompts from
templates and data, **check** them (required sections, escaping of untrusted text, token
budgets), **parse and validate** model responses, and **evaluate** recorded responses. Hidden
pytest tests check them in the `dojo-python` image. What tests cannot judge (is this
instruction clear?) is covered by each lesson's written exercise, which you can send for review
with `A`.

## Further reading

- Anthropic, "Prompt engineering overview": <https://docs.anthropic.com/en/docs/build-with-claude/prompt-engineering/overview>
- OpenAI, "Prompt engineering" guide: <https://platform.openai.com/docs/guides/prompt-engineering>
- Google, "Prompt design strategies" for Gemini: <https://ai.google.dev/gemini-api/docs/prompting-strategies>
- OpenAI, tiktoken (a tokenizer you can run locally): <https://github.com/openai/tiktoken>
- Vaswani et al., "Attention Is All You Need" (2017), the transformer paper: <https://arxiv.org/abs/1706.03762>
- roadmap.sh, Prompt Engineering roadmap: <https://roadmap.sh/prompt-engineering>
