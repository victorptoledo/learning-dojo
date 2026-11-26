# Checkpoint 3: Advanced

This checkpoint closes the **Advanced** phase. It has two parts, and you need both:

1. **The quiz** (press **Q**): eight questions over the whole phase. You need 7 right.
2. **The integration kata** (press **Enter**): a telehealth network gets a visits table
   partitioned by region that adopts last week's import, the right indexes for address lookups
   and prescriptions, a report of the indexes to drop, and roles that keep diagnoses, notes and
   other clinics out of reach.

Do them in any order. Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| Advanced Indexes | Which index type answers overlap and containment? When is an index partial, on an expression, or both? How do you find invalid and unused indexes? |
| Index Lab | Which operator does the query use, and how is the data laid out? |
| Declarative Partitioning | Why must keys include the partition key? What does ATTACH PARTITION do to the indexes and constraints of the parent? |
| Roles, Privileges and Row-Level Security | What can be granted per column? Who bypasses a policy? What does PUBLIC get on a new function? |
| Tenant Events | How does a policy fail closed when the setting is missing or empty? |
| Extensions and Connection Pooling | Which extensions need a restart? What breaks behind a transaction-mode pooler? |

> [!TIP]
> If a quiz question surprises you, open the lesson it comes from again before retrying. The
> explanation after each answer says which idea it tests.
