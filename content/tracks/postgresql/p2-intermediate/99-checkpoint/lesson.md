# Checkpoint 2: Intermediate

This checkpoint closes the **Intermediate** phase. It has two parts, and you need both:

1. **The quiz** (press **Q**): eight questions over the whole phase. You need 7 right.
2. **The integration kata** (press **Enter**): give a help desk an indexed full-text search
   with JSONB filters, an audit trail written by triggers, and a function that lists urgent
   tickets per plan.

Do them in any order. Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| Reading the Query Planner | Estimated against actual rows, loops, buffers: what do they tell you? When are extended statistics needed? |
| Locks in Depth | Which statements take ACCESS EXCLUSIVE? Why does a waiting ALTER block SELECTs? What is a root blocker? |
| JSONB and Full-Text Search | Which operators can a GIN index serve? Why name the configuration in to_tsvector? |
| PL/pgSQL | BEFORE or AFTER? What do NEW, OLD and TG_OP hold? Which volatility for a function that reads tables? |
| Plan Detective, Lock Detective | How do you read a plan's JSON, and how do you find who blocks whom? |

> [!TIP]
> If a quiz question surprises you, open the lesson it comes from again before retrying. The
> explanation after each answer says which idea it tests.
