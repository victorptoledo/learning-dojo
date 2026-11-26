# Checkpoint 3: Advanced

This checkpoint closes the **Advanced** phase. It has two parts, and you need both:

1. **The quiz** (press **Q**): eight questions over the whole phase. You need 7 right.
2. **The integration kata** (press **Enter**): a refund copilot. Emails are redacted before the
   model sees them and restored only in the reply, order notes are untrusted and withheld when
   they look like an injection, refunds above a limit (and every refund after an injection) wait
   for a human, transient errors are retried with backoff, a budget stops a run that costs too
   much, links are checked on the way out, and every model call and tool run leaves a span with
   no personal data in it.

Do them in any order. Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| Guardrails and Safety | Which threat is this: direct or indirect injection, data leak, excessive agency, unsafe output? Which layer stops it? |
| Guardrail Filter (kata) | What happens to the same value twice? What may an untrusted text change? |
| Cost and Latency | Which errors are worth retrying, and how long do you wait? Where does a budget check go? |
| Resilient Gateway (kata) | Why honour `retry-after`? What must never be retried? |
| Observability | What goes on a span, and what must never go there? How do you see a failed attempt? |

The kata is about **where** each check sits: before the model (redaction, untrusted content),
between the model and the world (tool policy), around each call (retries, budget, spans) and
after the model (output checks). A check in the wrong place is a check that does not hold.

> [!TIP]
> If a quiz question surprises you, open the lesson it comes from again before retrying. The
> explanation after each answer says which idea it tests.
