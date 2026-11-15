# Checkpoint 2: Intermediate

This checkpoint closes the **Intermediate** phase. It has two parts, and you need both:

1. **The quiz** (press **Q**): eight questions over the whole phase. You need 7 right.
2. **The integration kata** (press **Enter**): a home timeline with **hybrid fan-out**. Posts of
   normal accounts are pushed into bounded follower inboxes (a precomputed cache), posts of
   celebrities are pulled at read time, and the two are merged, filtered and paginated.

Do them in any order. Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| Caching Strategies | What is my staleness budget? Delete or set on write? How do I stop a stampede? |
| LRU + TTL Cache (kata) | What makes an entry recent? When is it expired? |
| Overload and Back-Pressure | Where do retries multiply? What do I shed first? What happens to work past its deadline? |
| Load-Shedding Queue (kata) | Which job is pushed out when the queue is full? |
| Read-Heavy Designs | Push, pull or hybrid? Where do I filter unfollows? Offset or cursor? |

> [!TIP]
> If a quiz question surprises you, open the lesson it comes from again before retrying. The
> explanation after each answer says which idea it tests.
