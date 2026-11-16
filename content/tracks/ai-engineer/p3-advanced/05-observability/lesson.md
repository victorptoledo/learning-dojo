# Observability for LLM Features: Traces, Metrics and Feedback

When a classic service fails, it throws, and your logs say where. When an LLM feature degrades,
nothing throws: answers just get a little worse, a little slower or a little more expensive.
**Observability** is what lets you see that, explain it and fix it: a record of every request
detailed enough to reconstruct what the model saw and did.

## What to record per model call

- model id (exact version) and provider, the **prompt template version**, and the parameters;
- input, output and cached **token counts**, and the computed cost;
- latency: total and time to first token;
- stop reason, errors, retries and fallbacks;
- for RAG: the query, the retrieved chunk ids and scores, what made it into the prompt;
- for agents: each step, tool name, arguments (redacted as needed), result size, errors;
- a request or conversation id tying it to the user action and to feedback.

Whether you also store the **full prompt and output** is a privacy decision (below). They are very
valuable for debugging and evaluation.

## Traces

Model calls are rarely alone: a request retrieves, reranks, calls the model, maybe calls tools and
the model again. A **distributed trace** shows this as a tree of **spans** with timings:

```text
POST /ask                                   2.9 s
├─ retrieve (hybrid, k=20)                  0.12 s
├─ rerank (top 5)                           0.25 s
├─ chat model-x  in=4,210 out=312 tokens    2.4 s
│  └─ tool get_policy(id="travel")          0.05 s
└─ check citations                          0.01 s
```

**OpenTelemetry** has semantic conventions for generative AI (`gen_ai.*` attributes such as the
operation, the provider, the request and response model, and input and output token usage), and
many SDKs and frameworks can emit them. Using the conventions means your traces work with standard
tools; dedicated LLM observability platforms (Langfuse, Arize Phoenix, LangSmith, Helicone and
others) add prompt views, cost dashboards and evaluation on top.

## Metrics and alerts

| Kind | Examples |
| --- | --- |
| Traffic and errors | requests, error rate by type (429, 5xx, timeouts), refusals, fallback rate |
| Latency | p50 / p95 / p99 total and time to first token, per route and model |
| Cost | tokens and cost per request, per feature, per tenant; cache hit rate |
| Quality proxies | thumbs up/down rate, edits of drafts, escalations, "not found" rate, invalid structured output, citation failures, online judge scores |
| Agents | steps per run, step-limit hits, tool error rate |

Alert on symptoms users feel (error rate, p95 latency, a drop in quality proxies) and on cost
anomalies (a tenant or an agent loop suddenly spending ten times more).

## Versions, feedback and drift

- **Version everything that shapes the output**: prompt templates, model ids, retrieval settings,
  the index snapshot. Record the versions on every trace so you can compare before and after.
- **Collect feedback** in the product and tie it to the trace: a thumbs-down with the exact
  prompt, passages and output is a ready-made eval case.
- **Online evaluation**: run cheap checks on all traffic (format, citations, length) and an LLM
  judge on a sample, with the same rubrics as offline.
- **Drift** happens without a deploy: the provider updates a model behind an alias, new documents
  change what retrieval returns, users start asking different questions. Pin model versions where
  you can, watch the quality proxies over time, and re-run the golden set on a schedule.

## Privacy and security in logs

Prompts contain personal data and sometimes secrets. Decide deliberately:

- redact or hash personal fields before they are stored; never log API keys;
- sample full payloads instead of storing all of them; set short retention;
- restrict access to prompt logs like any production data, and log who reads them;
- keep tenant data separate; respect deletion requests (data in traces counts).

## Summary

- Record model, prompt version, tokens, cost, latency, stop reasons and retrieval or tool details
  for every call.
- Trace whole requests with spans; use the OpenTelemetry GenAI conventions.
- Watch errors, latency percentiles, cost per request and tenant, and quality proxies; alert on
  symptoms and cost anomalies.
- Version prompts and models, tie feedback to traces, evaluate online and watch for drift.
- Treat prompt logs as sensitive data: redact, sample, retain briefly, restrict access.

## Further reading

- OpenTelemetry, semantic conventions for generative AI systems: <https://opentelemetry.io/docs/specs/semconv/gen-ai/>
- OpenTelemetry Python documentation: <https://opentelemetry.io/docs/languages/python/>
- Anthropic docs, OpenTelemetry: <https://platform.claude.com/docs/en/manage-claude/opentelemetry>
- OpenAI docs, production best practices: <https://platform.openai.com/docs/guides/production-best-practices>
- Langfuse documentation (open-source LLM observability): <https://langfuse.com/docs>
- Google SRE book, "Monitoring Distributed Systems" (the four golden signals): <https://sre.google/sre-book/monitoring-distributed-systems/>
