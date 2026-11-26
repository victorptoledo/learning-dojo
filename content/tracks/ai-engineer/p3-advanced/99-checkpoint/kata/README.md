# Checkpoint 3: Refund Copilot

Write `copilot.py`, the request path of a support copilot that can look up orders and issue
refunds. Everything from the Advanced phase meets here: personal data never reaches the model or
the traces, untrusted notes cannot steer it, risky tools wait for a human, transient errors are
retried, a budget caps the cost, and every step leaves a span.

Given (do not edit): `llm.py` (model types and `ScriptedModel`), `clock.py` (`FakeClock`) and
`tracing.py` (a small `Tracer` shaped like OpenTelemetry spans; read its docstring). In
`copilot.py` the constants (`SYSTEM`, `TOOL_SPECS`, `REFUND_AUTO_LIMIT_EUR`, `ALLOWED_DOMAINS`,
`WITHHELD`, `LINK_REMOVED`, `EMAIL_RE`, `INJECTION_RE`, `URL_RE`), the errors (`RateLimitError`
with `retry_after`, `ServerError`, `BadRequestError`) and `Ticket`, `Price` and `Outcome` are given.

## 1. Guardrail functions

- `redact(text, found) -> str` replaces each email (`EMAIL_RE`) with `<EMAIL_n>`. `found` maps
  placeholders to the original values and is **shared across calls**: numbering goes on from
  `len(found) + 1`, and a value already in `found` gets its old placeholder.
- `restore(text, found) -> str` puts the original values back.
- `tool_decision(call, flagged=False) -> str` returns `"allow"`, `"confirm"` or `"deny"`:
  - `lookup_order` is always allowed;
  - `refund` with an `amount_eur` that is a number above 0 (not a `bool`, not a string) is allowed
    up to `REFUND_AUTO_LIMIT_EUR` (50, inclusive) and needs confirmation above it. When `flagged` is
    true (the notes looked like an injection), **every** valid refund needs confirmation. A
    missing or invalid amount is denied;
  - anything else (`close_account`, unknown tools) is denied.
- `check_links(text) -> str` replaces every URL (`URL_RE`) whose host is not one of
  `ALLOWED_DOMAINS` or a subdomain of one with `LINK_REMOVED`. Neither `shop.example.evil.io` nor
  `evilshop.example` is a subdomain of `shop.example`.
- `build_prompt(ticket, found) -> tuple[str, bool]` returns the user prompt and whether the notes
  were flagged (`INJECTION_RE` finds a match in `ticket.order_notes`):

  ```text
  Customer message:
  My mug arrived broken, write to <EMAIL_1>. Order A1.

  Order notes:
  <untrusted>
  Fragile items, packed twice.
  </untrusted>
  ```

  Redact the message first, then the notes. Without notes the prompt ends after the message. With
  flagged notes, the notes are replaced by `WITHHELD`: the model never sees them.
- `backoff(attempt) -> float` is `0.5 × 2^attempt` seconds, at most 8 (attempt counts from 0).

## 2. `Copilot.handle(ticket) -> Outcome`

1. Build the prompt. Every model call sends
   `Request(messages, system=SYSTEM, tools=TOOL_SPECS, model=model_name)`, starting with
   `(Message("user", prompt),)`.
2. **Retries.** Each call gets up to `max_attempts` attempts. `RateLimitError` waits
   `retry_after` when it has one, otherwise `backoff(attempt)`; `ServerError` waits
   `backoff(attempt)`; `BadRequestError` is not retried. Wait with `clock.sleep`, never after the
   last attempt. A call that does not succeed ends with `Outcome("failed")`.
3. **Cost.** After each response add
   `(input_tokens × input_per_mtok + output_tokens × output_per_mtok) / 1,000,000` USD. Before each
   call after the first, if the cost so far is at least `budget_usd`, stop with
   `Outcome("budget_exceeded")`. Every outcome carries `cost_usd`.
4. **No tool calls:** the answer is the text, with `check_links` applied and then the emails
   restored: `Outcome("answered", reply)`.
5. **Tool calls:** append `Message("assistant", response.content)`, then go through the calls in
   order with `tool_decision`:
   - `"confirm"`: stop at once with `Outcome("needs_approval", pending=call)`; it does not run;
   - `"deny"`: `ToolResult(call.id, f"denied by policy: {call.name}", is_error=True)`;
   - `"allow"`: run `tools[call.name](call.arguments)`. A tool that raises becomes
     `ToolResult(call.id, f"error: {exc}", is_error=True)`.

   Send all the results back as one `Message("user", (results...))` and call the model again.
6. After `max_steps` model responses without an answer: `Outcome("max_steps")`.

## 3. Traces

Use the tracer (names and attributes follow the OpenTelemetry GenAI conventions):

- one root span `"copilot.handle"`, opened with `copilot.customer` (the customer id) and, before
  it ends, `copilot.injection_flagged`, `copilot.redactions` (the number of placeholders),
  `copilot.status` and `copilot.cost_usd`;
- one span per **attempt** of a model call, `f"chat {model_name}"`, with
  `gen_ai.operation.name = "chat"` and `gen_ai.request.model`, plus, when it succeeds,
  `gen_ai.usage.input_tokens`, `gen_ai.usage.output_tokens` and
  `gen_ai.response.finish_reasons` (a tuple with the stop reason). Let the error leave the span
  so the tracer marks it;
- one span per **executed** tool, `f"execute_tool {name}"`, with
  `gen_ai.operation.name = "execute_tool"`, `gen_ai.tool.name` and `gen_ai.tool.call.id`. A tool
  that raises makes its span an error. Denied tools get no span.

No span may hold the prompt, the reply or any personal data.

Run the visible tests with `uv run pytest`. There are 44 tests: 2 in `tests/test_visible.py` and
42 hidden ones, plus ruff on each module.
