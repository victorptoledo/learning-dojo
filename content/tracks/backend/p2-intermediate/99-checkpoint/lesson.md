# Checkpoint 2: Intermediate

This checkpoint closes the **Intermediate** phase. It has two parts, and you need both:

1. **The quiz** (press **Q**): eight questions over the whole phase. You need 7 right.
2. **The integration kata** (press **Enter**): order events published reliably through a
   transactional outbox, a relay that keeps order and parks poison events, and a consumer that ships
   each order exactly once although events arrive at least once.

Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| Transactions and NoSQL | What makes the order and its event atomic? What decides a duplicate insert race? |
| Caching and Queues | (Quiz) cache-aside, invalidation, stampedes, 202 Accepted. |
| Rate Limiter / Idempotent Payments | How do you make a repeated request harmless? |
| Messaging You Can Trust | Why an outbox? Why must the consumer be idempotent? What is a dead letter? |
