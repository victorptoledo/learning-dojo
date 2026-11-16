# How LLMs Work: Tokens, Context Windows and Sampling

You do not need the mathematics of transformers to build good AI features, but you do need an
accurate mental model of what happens in one request. It explains most of the surprises: why a
model "forgets", why a long prompt is slow and expensive, why the same question gets two answers.

## Tokens

A model does not read characters or words. Its **tokenizer** cuts text into **tokens**: frequent
words become one token, rare words several, and punctuation, spaces and code symbols their own.
Most tokenizers use byte-pair encoding (BPE) or a variant: start from bytes and repeatedly merge
the most frequent pairs seen in training data.

```text
"Tokenization isn't magic."  →  ["Token", "ization", " isn", "'t", " magic", "."]
```

Rules of thumb for English text: **one token is about 4 characters or 0.75 words**, so 1,000
words is roughly 1,300 tokens. Code, numbers, non-English languages and unusual words take
more tokens per character. Every model family has its own tokenizer, so the same text has a
different count on different models; providers offer a token-counting endpoint or library. Never
guess limits from characters in production code: count, or keep a safety margin.

Tokens explain some odd behaviour: models are weak at counting letters in a word or reversing
strings, because they never see the letters, only token ids.

## Next-token prediction

A language model is a function: given a sequence of tokens, it returns a **probability for every
token in its vocabulary** (often 100,000 or more) being the next one. Generation is a loop:

1. Run the model on the tokens so far.
2. Pick one next token from the distribution (sampling, below).
3. Append it and repeat, until a stop token, a stop sequence, or the `max_tokens` limit.

This is **autoregressive** generation. Consequences for engineers:

- Output is produced **one token at a time**, which is why APIs can **stream** it and why long
  answers take long: latency grows with output length.
- Reading the input (the "prefill") happens in parallel and is fast per token; writing the output
  ("decode") is sequential and slow per token. That is why **output tokens cost several times more
  than input tokens** on most price lists.
- The model has no memory between requests. A "conversation" is your code sending the whole
  history again every time.

Chat models are the same next-token predictors, trained further on conversations (instruction
tuning, reinforcement learning from human or AI feedback) so that they follow instructions, use a
role format (system, user, assistant) and call tools in a structured way. Some models also
**reason** before answering: they generate hidden or summarised "thinking" tokens first, which
improves hard tasks and costs more output tokens.

## The context window

The **context window** is the maximum number of tokens a model can handle in one request:
**input plus output**. Windows range from a few thousand tokens on small models to hundreds of
thousands or a million on large hosted ones. What fills it:

| Part | Typical size |
| --- | --- |
| System prompt and instructions | hundreds to a few thousand tokens |
| Tool definitions (name, description, JSON schema) | a few hundred tokens per tool |
| Conversation history | grows every turn |
| Retrieved documents and tool results | often the biggest part |
| Room for the answer (`max_tokens`) | reserved up front |

When the request does not fit, the API rejects it; it does not silently cut it. Your code must
decide what to drop. Common strategies: keep the system prompt and the latest turns and drop the
oldest; summarise old turns into a short note; retrieve only the relevant past turns or
documents. A bigger window is not free: every token is billed, slows the prefill, and models use
information in the middle of a very long context less reliably than at the start and end ("lost
in the middle"). **Put only what helps the task in the context.**

## Sampling: temperature, top-p, top-k

Choosing the next token from the distribution is **sampling**, controlled by a few parameters:

- **Temperature** rescales the probabilities. Low (0–0.3): the most likely tokens dominate,
  output is focused and repeatable. High (0.8–1.0+): flatter distribution, more varied and
  creative, more mistakes. Temperature 0 approximates **greedy decoding** (always the top token).
- **Top-p** (nucleus sampling) keeps only the smallest set of tokens whose probabilities add up to
  p (for example 0.9) and samples among them, cutting off the long tail of unlikely tokens.
- **Top-k** keeps only the k most likely tokens.
- **Stop sequences** end generation when a given string appears; **max_tokens** caps the output.

Even at temperature 0, **outputs are not guaranteed identical**: batching on the provider's GPUs,
floating-point order and model updates all introduce small differences, and one different token
early on changes everything after it. Design for variation: validate outputs, evaluate over sets,
and never depend on exact wording. Some newer models fix sampling internally and reject these
parameters, so check the model's documentation before relying on them.

## Tokens are the unit of cost and latency

Hosted models are priced **per million tokens**, with separate input and output prices:

```text
cost = input_tokens × input_price / 1e6  +  output_tokens × output_price / 1e6
```

Two latency numbers matter: **time to first token** (TTFT, mostly the prefill of your input) and
**output speed** (tokens per second during decode). A long prompt hurts TTFT; a long answer hurts
total time. Because a conversation resends its history, each turn's input grows, so the cost of a
long chat grows much faster than its length. **Prompt caching** (Advanced) makes a repeated prefix
cheaper and faster.

## Summary

- Models read and write tokens (about 4 characters of English each); counts differ per model, so
  count with the provider's tools.
- Generation is a loop of next-token predictions: streaming is natural, output is slow and
  expensive, and the model remembers nothing between requests.
- The context window holds input and output; when it is full, your code must drop, summarise or
  retrieve.
- Temperature, top-p and top-k shape sampling; outputs vary even at temperature 0.
- Cost and latency are driven by tokens: input for TTFT, output for total time and most of the bill.

## Further reading

- Vaswani et al., "Attention Is All You Need" (2017): <https://arxiv.org/abs/1706.03762>
- Sennrich et al., "Neural Machine Translation of Rare Words with Subword Units" (BPE, 2015): <https://arxiv.org/abs/1508.07909>
- Holtzman et al., "The Curious Case of Neural Text Degeneration" (nucleus sampling, 2019): <https://arxiv.org/abs/1904.09751>
- Liu et al., "Lost in the Middle: How Language Models Use Long Contexts" (2023): <https://arxiv.org/abs/2307.03172>
- Anthropic docs, context windows: <https://platform.claude.com/docs/en/build-with-claude/context-windows>
- Anthropic docs, token counting: <https://platform.claude.com/docs/en/build-with-claude/token-counting>
- OpenAI, tiktoken (an open-source BPE tokenizer): <https://github.com/openai/tiktoken>
- Andrej Karpathy, "Let's build the GPT Tokenizer" (video lecture): <https://www.youtube.com/watch?v=zduSFxRajkE>
