# Checkpoint 2: Intermediate

This checkpoint closes the **Intermediate** phase. It has two parts, and you need both:

1. **The quiz** (press **Q**): eight questions over the whole phase. You need 7 right.
2. **The integration kata** (press **Enter**): your own tiny orchestrator (a DAG with
   retries, a quality gate, resumable runs and backfills) and a daily orders pipeline of
   idempotent tasks that runs on it.

Do them in any order. Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| Slowly changing dimensions | Which type for a correction, which for a region? Why `IS DISTINCT FROM`? How does a point-in-time join work? |
| Change data capture | What can log-based CDC see that polling cannot? Why snapshot first? What does an outbox fix? |
| Data quality | Which checks catch a silent empty extract? What blocks publication and what only warns? |
| Orchestration | Why must tasks use the logical date? Which failures should never be retried? How do you backfill safely? |

> [!TIP]
> If a quiz question surprises you, open the lesson it comes from again before retrying. The
> explanation after each answer says which idea it tests.
