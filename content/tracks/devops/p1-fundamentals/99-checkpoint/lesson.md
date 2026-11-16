# Checkpoint 1: Fundamentals

This checkpoint closes the **Fundamentals** phase. It has two parts, and you need both:

1. **The quiz** (press **Q**): ten questions over the whole phase. You need 8 right.
2. **The integration kata** (press **Enter**): rewrite a draft GitHub Actions workflow into a
   production CI pipeline: lint, a test matrix, an image built and published only from `main`,
   least-privilege tokens, pinned actions, timeouts and cancelled superseded runs.

Do them in any order. Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| Linux for operations | In which order do you triage a sick host? What does state D mean? Where do OOM kills show up? |
| Containers for operations | What are namespaces and cgroups for? Why multi-stage? Why promote a digest instead of rebuilding? |
| CI pipelines | Why do jobs not share files? What is the cache key? Why pin actions to a SHA? What does concurrency do? |
| Configuration management | What makes a change idempotent? When does a handler run? What is drift and how do you find it? |
| Converge a host | How does a dry run differ from a real run, and how does a pipeline read its result? |

> [!TIP]
> If a quiz question surprises you, open the lesson it comes from again before retrying. The
> explanation after each answer says which idea it tests.
