# Checkpoint 4: Final Exam

This checkpoint closes the **Mastery** phase and the whole track. It has two parts, and you
need both:

1. **The quiz** (press **Q**): eight questions over the whole track, weighted towards smells,
   anti-patterns and choosing patterns. You need 7 right.
2. **The integration kata** (press **Enter**): a notification dispatcher buried under a
   singleton, a strategy per channel, a factory and a provider for the factory, with a type
   code switched on twice underneath. **Remove** what does not pay, replace the type code with
   a **sealed type**, and keep every behaviour under the characterisation tests.

Do them in any order. Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| Code Smells, Anti-Patterns and Refactoring Moves | Which smell is this, and which refactoring removes it? What does golden hammer look like in code? |
| Tame the God Class (kata) | Which small, safe step comes next? Are the tests still green? |
| Choosing, Combining and Removing Patterns | What force does this pattern answer here? What would the code look like without it? |
| Strategy and Factory | When is a lambda or a sealed type enough? |
| State, Visitor, Composite and Template Method with Sealed Types | What does an exhaustive `switch` give you over a visitor? |

> [!TIP]
> If a quiz question surprises you, open the lesson it comes from again before retrying. The
> explanation after each answer says which idea it tests.
