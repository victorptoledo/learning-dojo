# Checkpoint 4: Final Exam

This checkpoint closes the **Mastery** phase and the whole track. It has two parts, and you
need both:

1. **The quiz** (press **Q**): eight questions over the whole track, weighted towards
   governance, design review and what breaks APIs in production. You need 7 right.
2. **The integration kata** (press **Enter**): a billing API with six incident reports on
   its desk. A refactoring broke the mobile app, a list without bounds took the database
   down, a 500 leaked a connection string, a tenant read another tenant's invoices, retries
   charged customers twice, and an outage turned into a retry storm. Fix each one so that it
   cannot come back, without breaking the mobile app's contract again.

Do them in any order. Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| API Failure Modes in Production | Which failure is this? Which design rule prevents it, and which test proves it? |
| API Governance and Design Review | Which of these fixes could a linter or a contract test have caught before the merge? |
| Versioning and Evolution | Is this change compatible? What does the consumer's contract say? |
| Naming and Representation Conventions | How do I send money, days and instants? |
| Pagination, Filtering and Sorting | What is my default and maximum page size? What does my cursor encode? |
| Errors as Problem Details | What may an error say, and what must it never say? |
| API Keys, OAuth 2.x and Scopes | Whose object is this? |
| Idempotency Keys and Safe Retries | What do I remember, scoped to whom? Which failures leave the key free? |

> [!TIP]
> If a quiz question surprises you, open the lesson it comes from again before retrying. The
> explanation after each answer says which idea it tests.
