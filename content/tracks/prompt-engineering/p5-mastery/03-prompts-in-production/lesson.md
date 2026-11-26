# Prompts in Production

A prompt that passes its evaluation set has only started its life. In production it meets
inputs nobody imagined, a provider that changes, traffic spikes and budgets. This lesson
collects what goes wrong, how you notice, and what you build around the prompt so that a bad
moment does not become a bad day.

## Failure modes and their signals

| Failure | What you see | Signal to monitor |
| --- | --- | --- |
| **format drift** | parse errors, missing fields | parse/validation failure rate per prompt version |
| **truncation** | answers cut mid-sentence or invalid JSON | stop reason "max tokens", output token counts |
| **refusals** | "I can't help with that" on legitimate requests | refusal rate (a grader or a stop reason) |
| **hallucination** | confident wrong facts, invented citations | quote checks, sampled human review, user reports |
| **injection** | the assistant acts on text from a document | suspicious-content flags, output link checks |
| **latency** | slow responses, timeouts | p50/p95 latency, time to first token |
| **cost** | the bill grows faster than traffic | input/output tokens per request, cache hit rate |
| **model change** | everything shifts at once | model id per call; evaluation runs on new snapshots |

Two lessons hide in this table. First, **log the stop reason** of every call: it distinguishes a
model that finished from one that ran out of tokens or refused. Second, many of these failures
are **silent**: the answer arrives, it is just wrong. Only measurement shows them.

## Guardrails

Checks around the model, in code:

- **input checks**: size limits (a token budget per request), allowed languages or formats,
  flags for suspicious content;
- **output checks**: schema validation, business rules (an amount within limits, a date in the
  future), quote and citation checks, link allow-lists, a final check for personal data;
- **action checks**: permissions and confirmation for tools with side effects.

A guardrail that fails should lead to a defined behaviour: retry with the error, fall back, or
hand over to a human with a clear message.

## Fallbacks

Plan what happens when the model call fails or its output is unusable:

- **bounded retries** with the validation errors (Fundamentals);
- a **fallback model** (another snapshot or provider) behind the same prompt contract, evaluated
  in advance, not discovered during the incident;
- a **degraded mode**: a cached or template answer, the feature hidden, or a queue for a human;
- **timeouts and circuit breakers**, so a slow provider does not take your service down with it.

## Cost and latency

- **Shorter prompts**: remove dead instructions and redundant examples; measure what each part
  contributes.
- **Caching**: stable prefixes first (prompt caching), and cache whole answers where inputs
  repeat.
- **Routing**: a small, fast model for easy cases and a larger one for hard ones, decided by a
  classifier or by a confidence signal such as self-consistency agreement.
- **Output limits**: ask for the length you need and set `max_tokens` accordingly; reasoning
  models also have an effort or budget setting.
- **Batch APIs** for work that does not need an immediate answer, usually at a lower price.
- **Streaming** for perceived latency in chat interfaces.

## Privacy and data

Everything in the prompt is sent to the provider and may be logged by you. Send only the data
the task needs, mask or pseudonymise personal data where possible, know your provider's data
retention and training policies, and treat prompt logs as sensitive data with access control
and a retention limit.

## Three short cases

**The helpful date.** An extraction prompt filled a missing `due_date` with the invoice date,
because the schema required a string and nothing said what to do when it was absent. The fix
was a nullable field, an explicit instruction, and an evaluation slice of invoices without due
dates.

**The polite exfiltration.** A support assistant rendered Markdown. A customer pasted a
"diagnostic" text that made it answer with an image whose URL contained the conversation. The
fixes were plain-text rendering of untrusted-influenced answers, an output link allow-list, and
injection cases in the evaluation set.

**The quiet upgrade.** A team used a model alias. When it moved to a new generation, answers
became longer and a downstream length check started failing. Pinning the snapshot, alerting on
validation failure rates per model id, and an evaluation run before every model change turned
the next upgrade into a planned task.

## Where to go next

This track covered the prompt. Building complete systems around prompts (retrieval pipelines,
agents with many tools, evaluation pipelines in CI, observability) is the subject of the
**AI Engineer** track.

## Further reading

- Anthropic, "Reduce hallucinations": <https://docs.anthropic.com/en/docs/test-and-evaluate/strengthen-guardrails/reduce-hallucinations>
- Anthropic, "Reducing latency": <https://docs.anthropic.com/en/docs/test-and-evaluate/strengthen-guardrails/reduce-latency>
- OpenAI, "Production best practices": <https://platform.openai.com/docs/guides/production-best-practices>
- OpenAI, "Latency optimization": <https://platform.openai.com/docs/guides/latency-optimization>
- OWASP, "Top 10 for LLM Applications": <https://genai.owasp.org/llm-top-10/>
- Chip Huyen, "Building LLM applications for production": <https://huyenchip.com/2023/04/11/llm-engineering.html>
