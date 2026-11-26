# Checkpoint 5 🥋: Final Exam

This checkpoint closes the **Mastery** phase and the whole track. It has two parts, and you
need both:

1. **The quiz** (press **Q**): eight questions over the whole track, weighted towards builds,
   monorepos and soundness. You need 7 right.
2. **The integration kata** (press **Enter**): a feature-flag SDK published as a package.
   Its types come from `const` definitions, its overrides are validated by a zod schema built
   at runtime, its refreshes are async, atomic and safe against races and timeouts, its
   listeners are typed soundly, and `package.json` `exports` is its only way in.

Do them in any order. Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| TypeScript in Production | What does a consumer of a package see: sources, declarations or both? Why pin the compiler? |
| Monorepos, Workspaces and Project References | What does `exports` hide? What can a package import, and what can import it? |
| Soundness, Variance and Performance | Why is a method parameter checked bivariantly, and how do you avoid it? Which options close the other holes? |
| Utility and Conditional Types (P2) | How does a conditional type with `infer` pick the variants out of a definition? |
| Runtime Validation (P3) | How do you build a schema from data, and reject unknown keys? |
| Promises and Errors (P2) and Async Iteration (P4) | How do you time out a promise that ignores its signal? How do you know that a newer call started while you waited? |

> [!TIP]
> If a quiz question surprises you, open the lesson it comes from again before retrying. The
> explanation after each answer says which idea it tests.
