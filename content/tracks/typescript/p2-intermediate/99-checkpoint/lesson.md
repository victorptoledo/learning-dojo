# Checkpoint 2: Intermediate

This checkpoint closes the **Intermediate** phase. It has two parts, and you need both:

1. **The quiz** (press **Q**): eight questions over the whole phase. You need 7 right.
2. **The integration kata** (press **Enter**): an order pipeline over two modules, with a
   status union derived from a constant, a transition table checked with `satisfies`, a
   discriminated union of events, a custom error class and a concurrent, failure-tolerant
   `processAll`.

Do them in any order. Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| Utility and Conditional Types | What do `Omit`, `Partial` and `Record` produce? Why does a conditional type distribute over a union? How does `as` remove a key? |
| Enums, satisfies and Modules | Why a union instead of an `enum`? What does `satisfies` check, and what does it keep? When is a `.ts` file ESM? |
| Promises and Errors | When is `Promise.allSettled` the right call? What is the type of a `catch` variable, and how do you narrow it? When should a failure be a `Result`? |

> [!TIP]
> If a quiz question surprises you, open the lesson it comes from again before retrying. The
> explanation after each answer says which idea it tests.
