# Cost and Latency: Tokens, Caching, Routing, Retries and Rate Limits

A feature that works in the demo can still fail in production because it is too slow or too
expensive. Both are mostly a matter of **tokens**, and both have well-known levers. Measure first:
log input, output and cached tokens, latency and model for every call, then work on the biggest
numbers.

## Where the cost comes from

```text
cost per request = input_tokens × input_price + output_tokens × output_price  (prices per token)
daily cost       = cost per request × requests per day
```

Typical drivers:

- **Large, repeated input**: long system prompts, many retrieved passages, tool definitions, the
  whole conversation history resent every turn.
- **Output**: priced several times higher per token; reasoning models also bill their thinking.
- **Multiplication**: agent loops and multi-step workflows make several calls per user request;
  retries and judges add more.
- **The model choice**: prices between the smallest and the largest models of one provider differ
  by more than ten times.

Judge cost per **completed task**, not per request: a cheaper model that needs three attempts is not
cheaper.

## The levers, roughly in order

1. **Send fewer tokens.** Trim instructions, retrieve fewer but better passages (reranking),
   summarise or drop old history, return compact tool results, set sensible `max_tokens` and ask
   for concise answers.
2. **Prompt caching.** Providers cache the processed prefix of a prompt: a repeated prefix (system
   prompt, tool definitions, a long document) is billed at a fraction of the input price and
   processed faster. Put **stable content first** and variable content (the question, timestamps)
   last; any change in the prefix invalidates the cache after it. Check the cached-token counts in
   the usage to be sure it works. (Anthropic uses explicit `cache_control` breakpoints or automatic
   caching; OpenAI and Gemini cache long prefixes automatically or on request.)
3. **The right model per task.** Use a small, fast model for classification, extraction, routing
   and query rewriting, and the large one only where it measurably helps. A **router** (rules or a
   cheap classifier) can send easy requests to the small model. On reasoning models, a lower
   effort or thinking setting is the same lever inside one model.
4. **Batch APIs** for anything that is not interactive (nightly enrichment, evals, backfills): about
   half price for results within hours.
5. **Cache whole answers** for identical (or, with care, very similar) requests, keyed by a hash of
   the normalised request and the model and prompt version. Semantic caching by embedding
   similarity is riskier: two similar questions can need different answers.

## Latency

- **Time to first token** grows with input length and model size; **total time** grows with output
  length. Shorter prompts and answers help both.
- **Stream** responses in user-facing features: the user starts reading after the first token.
- **Parallelise** independent calls (retrieval and a classifier, several tool calls, per-document
  summaries) with asyncio or threads, within your rate limits.
- **Smaller models** are faster per token; some providers offer faster tiers or regions.
- Avoid unnecessary steps: an agent loop of eight calls is eight round trips.

Set **timeouts** everywhere and know your latency budget: if the whole page must answer in 3
seconds, a 10-second model call needs streaming, a smaller model, or a different design.

## Rate limits

Providers limit **requests per minute** and **tokens per minute** (input and output, sometimes
separately), per organisation and model. Exceeding them returns 429, often with a `retry-after`
header. In your own service:

- keep a **client-side limiter** (a token bucket that refills at the allowed rate) so you smooth
  bursts instead of hammering the API;
- give each tenant or user a quota so one customer cannot exhaust everyone's capacity;
- queue background work and let interactive traffic go first.

## Retries, backoff and fallbacks

Retry only what can succeed later: 429, 5xx, overloaded and network errors (Fundamentals). Use
**exponential backoff with jitter**:

```text
delay(attempt) = random(0, min(cap, base × 2^attempt))      # "full jitter"
```

Without jitter, thousands of clients that failed together retry together and fail again. Honour
`retry-after` when present. Cap the attempts and the total time. When the primary model keeps
failing, **fall back** to another model or provider (which must be evaluated too), or degrade
gracefully: a cached answer, a simpler feature, a clear message. A **circuit breaker** stops sending
requests to a failing provider for a while instead of piling up timeouts.

## Summary

- Measure tokens, cost and latency per call; optimise the biggest numbers; judge cost per completed
  task.
- Levers: fewer tokens, prompt caching with a stable prefix, the right model per task, batch APIs,
  answer caching.
- Latency: shorter prompts and answers, streaming, parallel calls, smaller models, timeouts.
- Respect rate limits with client-side token buckets and per-tenant quotas.
- Retry transient errors with capped exponential backoff and jitter, honour retry-after, and fall back
  or degrade gracefully.

## Further reading

- Anthropic docs, prompt caching: <https://platform.claude.com/docs/en/build-with-claude/prompt-caching>
- Anthropic docs, rate limits: <https://platform.claude.com/docs/en/api/rate-limits>
- Anthropic docs, batch processing: <https://platform.claude.com/docs/en/build-with-claude/batch-processing>
- OpenAI docs, prompt caching: <https://platform.openai.com/docs/guides/prompt-caching>
- OpenAI docs, latency optimization: <https://platform.openai.com/docs/guides/latency-optimization>
- AWS Architecture Blog, "Exponential Backoff and Jitter": <https://aws.amazon.com/blogs/architecture/exponential-backoff-and-jitter/>
- Chen et al., "FrugalGPT: How to Use Large Language Models While Reducing Cost and Improving Performance" (2023): <https://arxiv.org/abs/2305.05176>
