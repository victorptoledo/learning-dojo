# Checkpoint 2: Intermediate

This checkpoint closes the **Intermediate** phase. It has two parts, and you need both:

1. **The quiz** (press **Q**): eight questions over the whole phase. You need 7 right.
2. **The integration kata** (press **Enter**): a quote service with seven positional
   parameters, five hard-wired pricing rules and two hard-wired parties becomes a validated
   **builder**, a **pipeline** of pricing steps and **observers** with subscriptions that
   cannot break each other.

Do them in any order. Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| Creating Objects | Constructor, static factory, record or builder? Why is `getInstance()` a smell? |
| Observer, Command and Chain of Responsibility | How do listeners leave, and what if one throws? What does undo need? |
| State, Visitor, Composite and Template Method | Which data does each state carry? New type or new operation: which is cheap here? |
| Order State (kata) | Where are illegal transitions refused? Why no `default`? |
| Editor Commands (kata) | When is the redo stack cleared? How does a macro roll back? |

> [!TIP]
> If a quiz question surprises you, open the lesson it comes from again before retrying. The
> explanation after each answer says which idea it tests.
