# Stream Assembler

A model API streams its answer as **server-sent events** (SSE). Write `stream.py`, which turns the
raw lines into the `Response` type of `llm.py` (given, do not edit), and lets a UI show text as it
arrives.

## 1. `parse_sse(lines)`

`lines` is an iterable of text lines (with or without `"\n"` or `"\r\n"` at the end). Yield one
`dict` per event, lazily (a generator):

- `data: <value>` adds a data line; one space after the colon is removed. An event may have several
  data lines: join them with `"\n"`.
- An **empty line** ends the event: decode the joined data with `json.loads` and yield it. An empty
  line with no data lines yields nothing.
- Lines starting with `:` are comments (keep-alives): ignore them. Ignore other fields
  (`event:`, `id:`, `retry:`): the event type is inside the JSON.
- Data that is not valid JSON raises `StreamError`.
- At the end of the input, data not followed by an empty line is an **incomplete event**: drop it.

## 2. `assemble(events)`

`events` is an iterable of event dicts (as `parse_sse` yields them). The types:

| Event | Fields | Meaning |
| --- | --- | --- |
| `message_start` | `usage.input_tokens` | the answer starts |
| `block_start` | `index`, `block`: `{"type": "text"}` or `{"type": "tool_call", "id", "name"}` | a content block starts |
| `text_delta` | `index`, `text` | more text for a text block |
| `arguments_delta` | `index`, `json` | more of a tool call's arguments, as a fragment of a JSON string |
| `block_stop` | `index` | the block is complete |
| `message_stop` | `stop_reason`, `usage.output_tokens` | the answer is complete |
| `ping` | | keep-alive: ignore |
| `error` | `error.type`, `error.message` | the provider failed mid-stream |

Return a `Response` whose `content` has one `TextBlock` or `ToolCall` per block, **ordered by
index**, with the joined text, or the tool call's `id`, `name` and `arguments` (the joined JSON
fragments decoded into a dict; no fragments means `{}`), the `stop_reason` and
`Usage(input_tokens, output_tokens)`.

Raise `StreamError` when:

- an `error` event arrives (the message must contain the error's type and message);
- a delta names an index that was never started, or the wrong kind of delta for its block;
- a tool call's arguments are not valid JSON or not a JSON object;
- the events end before `message_stop`.

Events after `message_stop` are ignored.

## 3. `stream_text(events, on_text)`

Assemble the response like `assemble`, and call `on_text(text)` for every text delta **as soon
as it arrives**, before later events are read. Return the `Response`.

```python
response = stream_text(parse_sse(lines), on_text=lambda t: print(t, end=""))
```

Run the visible tests with `uv run pytest`. There are 21 tests: 2 in `tests/test_visible.py`
and 19 hidden ones, plus ruff on each module.
