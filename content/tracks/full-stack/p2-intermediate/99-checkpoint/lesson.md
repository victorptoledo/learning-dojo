# Checkpoint 2: Intermediate

This checkpoint closes the **Intermediate** phase. It has two parts, and you need both:

1. **The quiz** (press **Q**): ten questions over the whole phase. You need 8 right.
2. **The integration kata** (press **Enter**): a comments feature in Spring Boot, from the JPA
   entity to what the page needs: newest-first pages with opaque cursors, errors that name their
   field, and a `canDelete` flag that the server also enforces.

Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| Authentication Across the Stack | Who may delete a comment? Where is that decided, and where is it only shown? |
| Data Fetching and Caching | How does a client know a next page exists? Why must cursors be opaque? |
| Rendering Strategies | (Quiz only) SSG, ISR, SSR, hydration and server components. |
| From Screens to Tables (phase 1) | Which query and index serve "one article, newest first"? |

> [!TIP]
> Ask the repository for one row more than the page: if it comes back, there is a next page,
> and you never need a `count(*)`.
