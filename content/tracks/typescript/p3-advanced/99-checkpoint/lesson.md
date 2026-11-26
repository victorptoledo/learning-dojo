# Checkpoint 3: Advanced

This checkpoint closes the **Advanced** phase. It has two parts, and you need both:

1. **The quiz** (press **Q**): eight questions over the whole phase. You need 7 right.
2. **The integration kata** (press **Enter**): a webhook inbox. You type an untyped JavaScript
   module with a declaration file, validate the events with a zod discriminated union whose
   types are inferred, dispatch each event to handlers typed by its event type and drop
   duplicates, including the one that arrives while the first delivery is still running.

Do them in any order. Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| Runtime Validation at the Boundaries | Where does untrusted data enter? What is the difference between `z.input` and `z.output`? How does a brand stop a plain string from posing as an id? |
| Validate at the Boundary (kata) | How do you turn zod issues into `"path: message"` strings? |
| Declaration Files and Augmentation | What does a `.d.ts` file promise, and who checks that promise? What happens when a JavaScript module has no types at all? |
| Testing, Type-Level Tests and Linting | What does `Expect<Equal<A, B>>` prove? Why must an `@ts-expect-error` line really be an error? |
| Typed Events (kata) | How does a generic method over `keyof` tie a name to its payload? Where does the one unavoidable cast live? |

> [!TIP]
> If a quiz question surprises you, open the lesson it comes from again before retrying. The
> explanation after each answer says which idea it tests.
