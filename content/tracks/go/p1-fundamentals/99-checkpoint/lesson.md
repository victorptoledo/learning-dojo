# Checkpoint 1: Fundamentals

This checkpoint closes the **Fundamentals** phase. It has two parts, and you need both:

1. **The quiz** (press **Q**): eight questions over the whole phase. You need 7 right.
2. **The integration kata** (press **Enter**): a bank ledger with structs and pointer
   receivers, a map of accounts, money in cents, amounts parsed from text, and errors wrapped
   with `%w` so callers can test them with `errors.Is` and `errors.As`.

Do them in any order. Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| The go Command and Modules | What do the `go` line and go.sum do? Which names does a package export? |
| Types, Slices and Maps | What does a slice header hold, and when do two slices share memory? What does a nil map allow? |
| Structs, Methods and Embedding | When must a receiver be a pointer? What does embedding promote, and what does it not make? |
| Errors as Values | Why `%w` and not `%v`? When `errors.Is`, when `errors.As`? Where is an error logged? |

> [!TIP]
> If a quiz question surprises you, open the lesson it comes from again before retrying. The
> explanation after each answer says which idea it tests.
