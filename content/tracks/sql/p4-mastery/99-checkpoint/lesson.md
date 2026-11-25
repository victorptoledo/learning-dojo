# Checkpoint 4: Final Exam

This checkpoint closes the **Mastery** phase and the whole track. It has two parts, and you
need both:

1. **The quiz** (press **Q**): eight questions over the advanced phases, weighted towards
   anti-patterns and SQL inside applications. You need 7 right.
2. **The integration kata** (press **Enter**): an inherited billing module. Migrate float money
   and a list of ids in a text column to a proper schema, close a month in one set-based
   statement that can run twice, rewrite a search that pastes input into SQL, and replace an
   N+1 page with one query.

Do them in any order. Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| Common SQL Anti-Patterns | Which of these gives wrong results and which only slow ones? Why does `BETWEEN` fail on timestamps? What does fan-out do to a sum? Why is a list of ids in a column a smell? |
| SQL from Applications | What travels separately from the SQL text with a bind parameter? How do you spot and remove an N+1? Why keysets and not `OFFSET`? |
| Query Clinic (kata) | Which symptom points to which anti-pattern? |
| Transactions, Isolation and Locking | What makes a batch safe to run twice? |
| Joins of Every Kind | When do you need `LATERAL`, and what does `LEFT JOIN ... ON true` keep? |

> [!TIP]
> If a quiz question surprises you, open the lesson it comes from again before retrying. The
> explanation after each answer says which idea it tests.
