# Checkpoint 2: Intermediate

This checkpoint closes the **Intermediate** phase. It has two parts, and you need both:

1. **The quiz** (press **Q**): eight questions over the whole phase. You need 7 right.
2. **The integration kata** (press **Enter**): an order service that keeps stock consistent with
   a transaction, reads orders with their lines in one query, and answers every failure clearly.

Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| Spring Data JPA | When does a transaction roll back? Do I need `save` for a managed entity? |
| Relationships and Queries | How do I load an order with its lines in one query, outside a lazy trap? |
| Testing | What does each grading test set up, and what does it read back? |
| HTTP clients | (Quiz only) timeouts, retries and error mapping. |

> [!TIP]
> The grading tests place an order whose **second** line fails and then check the stock of the
> **first** line. Only a real transaction makes that pass.
