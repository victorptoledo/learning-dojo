# Checkpoint 2: Intermediate

This checkpoint closes the **Intermediate** phase. It has two parts, and you need both:

1. **The quiz** (press **Q**): eight questions over the whole phase. You need 7 right.
2. **The integration kata** (press **Enter**): an inventory service that imports CSV uploads
   line by line as they stream in and streams its inventory back as NDJSON, with central
   error handling.

Do them in any order. Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| Streams and Buffers | Why can a character be split between chunks? What does `pipeline` do that `pipe` does not? What happens when `write()` returns `false`? |
| HTTP Servers and Clients | What are `req` and `res`? Why one `.catch` around the router? Which status for a too-large body, a wrong content type, invalid fields? Which timeout does `fetch` have? |
| Errors, Configuration and Testing | Operational or programmer error, and what to do with each? Why validate config at startup? How do you test a server and a timer? |

> [!TIP]
> If a quiz question surprises you, open the lesson it comes from again before retrying. The
> explanation after each answer says which idea it tests.
