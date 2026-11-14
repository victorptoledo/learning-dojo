# Checkpoint 1: Fundamentals

This checkpoint closes the **Fundamentals** phase. It has two parts, and you need both:

1. **The quiz** (press **Q**): eight questions over the whole phase. You need 7 right.
2. **The integration kata** (press **Enter**): an issue tracker API with resources nested under
   projects, filtering and pagination, JSON merge patch, and optimistic concurrency with ETags.

Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| HTTP in Depth | What do `If-Match`, `412` and `428` mean? Which header carries the version? |
| REST API Design | How are issues addressed? How do merge patch and pagination work? |
| Relational Modelling | Which constraint keeps issue numbers unique per project? |
| Authentication | (Quiz only) sessions versus tokens, JWT verification, OAuth2 flows. |

> [!TIP]
> The entity's `@Version` column is the version clients see as the ETag. Compare it yourself
> before changing anything, and Hibernate will also refuse a concurrent update.
