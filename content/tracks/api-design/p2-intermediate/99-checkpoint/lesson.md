# Checkpoint 2: Intermediate

This checkpoint closes the **Intermediate** phase. It has two parts, and you need both:

1. **The quiz** (press **Q**): eight questions over the whole phase. You need 7 right.
2. **The integration kata** (press **Enter**): a customers API whose new model broke an old
   mobile client. Bring back version 1 exactly as the client's contract test expects, computed
   from the new model; serve the new model as version 2; announce the end of version 1 with
   `Deprecation`, `Sunset` and successor links; and give each representation its own `ETag`.

Do them in any order. Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| Cursor Feed (kata) | What does my cursor encode? Why does it survive inserts? Which query is it bound to? |
| Versioning and Evolution | Is this change compatible? Can I expand, then contract? Which headers announce a sunset? |
| Idempotency Keys and Safe Retries | Which key, scoped to whom? Which outcomes do I store, and for how long? |
| Idempotency Keys (kata) | What makes "is this key new?" atomic? When do I release a key? |
| Caching and Conditional Requests | `no-cache` or `no-store`? Weak or strong comparison? `304`, `412` or `428`? |
| Conditional Requests (kata) | What in my resource changes exactly when its representation changes? |

> [!TIP]
> If a quiz question surprises you, open the lesson it comes from again before retrying. The
> explanation after each answer says which idea it tests.
