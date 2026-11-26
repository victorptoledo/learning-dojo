# Checkpoint 4: Final Exam

This checkpoint closes the **Mastery** phase and the whole track. It has two parts, and you
need both:

1. **The quiz** (press **Q**): eight questions on architecture, testing across the stack and
   production ownership, the decisions a full stack lead is asked to justify. You need 7 right.
2. **The integration kata** (press **Enter**): the expand release of a breaking change, splitting
   `full_name` into `first_name` and `last_name` while the old release, cached web apps and
   slow-updating mobile apps keep running: a migration runner, an additive migration, dual
   writes, a batched backfill, a flag with a kill switch and a stable rollout, and a consumer
   contract that proves old clients still work.

Do them in any order. Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| Full Stack Architecture | Which forces justify services over a modular monolith? Where does a BFF belong? |
| Testing Across the Stack | What does each test level prove, and what does it fake? What does a contract test protect? |
| Production Ownership | What are the deploys of expand and contract, in order? How do you roll back without undoing the database? |
| From Screens to Tables (phase 1) | What does a migration do to rows the old code still writes? |
| Typed API Contracts (phase 1) | Which changes to a response break a client, and which do not? |

> [!TIP]
> Every line of this kata answers one question: "what is still running while this deploys?"
> If a change only works once everything is new, it belongs in the contract release, not here.
