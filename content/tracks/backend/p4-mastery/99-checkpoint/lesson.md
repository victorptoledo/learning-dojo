# Checkpoint 4: Final Exam

This checkpoint closes the **Mastery** phase and the whole track. It has two parts, and you need
both:

1. **The quiz** (press **Q**): eight questions over the whole track, weighted towards architecture,
   delivery and engineering leadership. You need 7 right.
2. **The integration kata** (press **Enter**): the checkout moves out of the monolith. You write an
   orchestrated saga with compensations that survives a crash halfway, rename a column with expand
   and contract (both writers, a batched backfill, reads that fall back), and route users between
   the old and the new checkout with a percentage rollout.

Do them in any order. Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| System Architecture | Why can a checkout across three services not be one transaction? What does each compensation undo, and in which order? |
| Delivery: From Commit to Production | Which releases does a column rename take? Why must every writer fill both columns? How does a flag roll out to 5% and then back to 0? |
| Engineering Leadership | How would you write this migration down as an ADR, and what would you look for when reviewing it? |
| Messaging You Can Trust / Idempotent Payments | What makes a step safe to run twice after a crash? |

> [!TIP]
> If a quiz question surprises you, open the lesson it comes from again before retrying. The
> explanation after each answer says which idea it tests.
