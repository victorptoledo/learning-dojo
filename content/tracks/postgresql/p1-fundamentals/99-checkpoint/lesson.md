# Checkpoint 1: Fundamentals

This checkpoint closes the **Fundamentals** phase. It has two parts, and you need both:

1. **The quiz** (press **Q**): eight questions over the whole phase. You need 7 right.
2. **The integration kata** (press **Enter**): turn a meeting-room booking table into one the
   database keeps correct, with ranges, a domain, checks and an exclusion constraint, then
   query free rooms and busy periods and configure a role and autovacuum.

Do them in any order. Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| PostgreSQL Types | Why half-open ranges? What does an exclusion constraint compare, and why does it need btree_gist? |
| MVCC Inside | What do xmin and xmax hold? When is an update HOT? |
| VACUUM and Autovacuum | When does autovacuum trigger? What holds back the xmin horizon? What is wraparound? |
| Configuration | Which level wins? Which settings need a restart? Why is work_mem per operation? |
| Catalog Tour, Vacuum Clinic | Where do reloptions and role settings live in the catalogs? |

> [!TIP]
> If a quiz question surprises you, open the lesson it comes from again before retrying. The
> explanation after each answer says which idea it tests.
