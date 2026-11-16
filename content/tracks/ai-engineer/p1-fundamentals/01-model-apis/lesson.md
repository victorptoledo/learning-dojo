# Calling Model APIs: Messages, Streaming, Stop Reasons and Errors

Every hosted model (Anthropic's Claude, OpenAI's GPT models, Google's Gemini, Mistral and the many
open-weight models behind compatible servers) is reached through an HTTP API with the same
shape: you send a list of **messages**, you get back **content**, a **stop reason** and **usage**.
Learn the shape once and every SDK becomes familiar.

## The request

```json
{
  "model": "a-model-id",
  "system": "You answer questions about our shipping policy. Be brief.",
  "messages": [
    {"role": "user", "content": "Can I return shoes after 40 days?"},
    {"role": "assistant", "content": "Returns are accepted within 30 days."},
    {"role": "user", "content": "Even if they are unworn?"}
  ],
  "max_tokens": 500,
  "temperature": 0.2,
  "stop_sequences": ["\n\nUser:"],
  "tools": []
}
```

- **model**: an exact id from the provider's model list. Keep it in configuration: you will change
  it, and old models are retired on a published schedule.
- **system**: the instructions that frame the whole conversation. Some APIs take it as a separate
  field (Anthropic, Gemini), others as a first message with role `system` or `developer` (OpenAI).
- **messages**: alternating `user` and `assistant` turns. The API is **stateless**: you send the
  history every time (some providers offer server-side conversation state as an option).
- **max_tokens**: the cap on the answer. Required on some APIs. Too low truncates answers.
- **sampling** (`temperature`, `top_p`): optional; some reasoning models fix or reject them.
- **tools**: functions the model may ask you to call (Intermediate).

Content is not only text: a message can hold several **content blocks** (text, images, documents,
tool calls, tool results).

## The response

```json
{
  "id": "msg_123",
  "model": "a-model-id",
  "content": [{"type": "text", "text": "Unworn shoes can be returned within 30 days."}],
  "stop_reason": "end_turn",
  "usage": {"input_tokens": 61, "output_tokens": 12}
}
```

The same idea has different names:

| Idea | Anthropic Messages | OpenAI Chat Completions | Gemini |
| --- | --- | --- | --- |
| answer | `content` blocks | `choices[0].message` | `candidates[0].content.parts` |
| finished normally | `stop_reason: end_turn` | `finish_reason: stop` | `finishReason: STOP` |
| hit the token cap | `max_tokens` | `length` | `MAX_TOKENS` |
| wants a tool | `tool_use` | `tool_calls` | a `functionCall` part |
| declined or filtered | `refusal` | `content_filter` | `SAFETY` |
| usage | `usage.input_tokens/output_tokens` | `usage.prompt_tokens/completion_tokens` | `usageMetadata` |

The katas use their own small, provider-neutral types (`Response`, `TextBlock`, `ToolCall`,
`Usage`, stop reasons `end_turn`, `max_tokens`, `tool_call`, `stop_sequence`, `refusal`) so that
what you learn maps onto any SDK.

## Stop reasons are part of the contract

Never read `content` without checking why the model stopped:

- **end_turn / stop**: a complete answer.
- **max_tokens / length**: the answer is **cut off**. Truncated JSON will not parse; a truncated
  summary misleads. Raise `max_tokens`, ask for less, or continue in a new request; never treat it
  as success.
- **stop_sequence**: one of your stop strings appeared; expected when you set them.
- **tool_call / tool_use**: the model wants you to run a tool and send the result back.
- **refusal / content_filter / SAFETY**: the model or a safety system declined. Show a helpful
  message, log it, and do not retry the identical request in a loop.

## Streaming

With `"stream": true` the API answers with **server-sent events** (SSE): a long-lived HTTP
response where each event is a few `field: value` lines followed by a blank line.

```text
event: content_block_delta
data: {"type":"content_block_delta","index":0,"delta":{"type":"text_delta","text":"Unworn"}}

event: content_block_delta
data: {"type":"content_block_delta","index":0,"delta":{"type":"text_delta","text":" shoes"}}

event: message_delta
data: {"type":"message_delta","delta":{"stop_reason":"end_turn"},"usage":{"output_tokens":12}}
```

Streaming does not make the model faster; it shows the **first words after the time to first
token** instead of after the whole answer, which matters for chat UIs and long outputs. It also
avoids HTTP timeouts on very long generations. The cost: you must assemble the final message
yourself (or use the SDK's helper), cope with tool-call arguments that arrive as partial JSON
strings, and handle errors that arrive **in the middle** of a stream. In the next kata you write
that assembler.

## Errors: which to retry

| Status | Meaning | Retry? |
| --- | --- | --- |
| 400 | Invalid request (bad parameter, too many tokens for the window) | No: fix the request |
| 401 / 403 | Bad key or no permission | No |
| 404 | Unknown model or endpoint (often a retired model id) | No |
| 408 / connection reset / timeout | Network trouble | Yes, with backoff |
| 429 | Rate limited (requests or tokens per minute) | Yes, after `retry-after` if given |
| 500 / 502 / 503 | Provider error | Yes, with backoff |
| 529 (Anthropic) / 503 overloaded | Provider overloaded | Yes, with backoff; consider a fallback |

Official SDKs already retry a few times with exponential backoff and expose typed exceptions
(`RateLimitError`, `APIConnectionError` ...). Catch the specific classes, not one broad
exception: retrying a 400 forever wastes money and hides a bug. Set a **timeout** on every call;
reasoning models and long outputs can take minutes, so use streaming for long generations
rather than a huge timeout.

## Keys, configuration and testability

- API keys come from the environment or a secret manager, never from code or the repository. Use
  one key per environment and service, so you can rotate and attribute spending.
- Never call a model API with a key from a browser or mobile app: put your own backend in between.
- Wrap the SDK in a **thin client of your own** with a provider-neutral request and response. The
  rest of the code depends on that interface, tests use a fake that returns scripted responses,
  and switching provider or model touches one module.
- Log, per call: model, latency, input and output tokens, stop reason, a request id. Logging
  full prompts and answers is useful but may contain personal data (Advanced).

## Batch and other endpoints

Most providers also offer a **batch API** (send thousands of requests, get results within hours at
about half the price), **token counting**, **embeddings** (next lessons) and a **models list**.
Use batch for offline work: backfills, nightly evals, bulk classification.

## Summary

- Request: model, system, messages, max_tokens, optional sampling and tools. Response: content
  blocks, stop reason, usage. The names differ per provider; the shape does not.
- Always check the stop reason: truncation and refusals are not successes.
- Streaming improves perceived latency and long outputs, but you assemble the result and handle
  mid-stream errors.
- Retry 429, 5xx, overloaded and network errors with backoff; never retry 4xx request errors.
- Keys from the environment, model ids from config, and a thin client you can fake.

## Further reading

- Anthropic API reference, Messages: <https://platform.claude.com/docs/en/api/messages/create>
- Anthropic docs, streaming messages: <https://platform.claude.com/docs/en/build-with-claude/streaming>
- Anthropic API reference, errors: <https://platform.claude.com/docs/en/api/errors>
- OpenAI API reference, Chat Completions: <https://platform.openai.com/docs/api-reference/chat>
- OpenAI docs, streaming responses: <https://platform.openai.com/docs/guides/streaming-responses>
- Google, Gemini API text generation: <https://ai.google.dev/gemini-api/docs/text-generation>
- WHATWG HTML standard, server-sent events: <https://html.spec.whatwg.org/multipage/server-sent-events.html>
