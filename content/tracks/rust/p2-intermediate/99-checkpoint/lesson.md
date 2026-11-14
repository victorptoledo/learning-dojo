# Checkpoint 2: Intermediate

This checkpoint closes the **Intermediate** phase. You need both parts:

1. **The quiz** (press **Q**): eight questions over the phase. You need 7 right.
2. **The integration kata** (press **Enter**): a grade book that parses text into structs,
   reports errors with a typed `Result`, implements a trait and ranks students with iterators.

Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| Option and Result | When is `?` allowed? How do I turn an `Option` into a `Result`? |
| Traits and Generics | How do I implement `Display`? What does a trait bound promise? |
| Config Parser | How did I attach the line number to an error? |
| Iterators and Closures | Can I write the loop as a `filter`/`map`/`collect` chain? |
| Modules and Tests | What can an integration test in `tests/` see? |

> [!TIP]
> Write a failing test first for each rule in the statement, then make it pass. The visible tests
> are only a start: the hidden ones check every error case.
