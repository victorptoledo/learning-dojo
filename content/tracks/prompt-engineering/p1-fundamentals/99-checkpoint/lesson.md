# Checkpoint 1: Fundamentals

This checkpoint closes the **Fundamentals** phase. It has two parts, and you need both:

1. **The quiz** (press **Q**): eight questions over the whole phase. You need 7 right.
2. **The integration kata** (press **Enter**): the code around a ticket-triage prompt. A
   system prompt you write, tickets escaped into tags, few-shot examples as conversation turns,
   a parser that validates the model's JSON, and a retry message built from the problems.

Do them in any order. Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| Clear Instructions and Context | What are my success criteria? What is the model's way out? Why give reasons? |
| Roles and System Prompts | What is stable (system) and what changes per call (user)? Where are rules enforced? |
| Examples and Few-Shot | Are the examples balanced, interleaved and separate from the test data? |
| Structured Output | Enums, explicit nulls, unknown fields? What does the parser do with a code fence? |

> [!TIP]
> Write your own system prompt in the kata with care: the tests check that it names every
> category, priority and field, but a reviewer (press `A`) can judge whether it is clear.
