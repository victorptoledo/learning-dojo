# Checkpoint 1: Fundamentals

This checkpoint closes the **Fundamentals** phase. It has two parts, and you need both:

1. **The quiz** (press **Q**): eight questions over the whole phase. You need 7 right.
2. **The integration kata** (press **Enter**): an orders API with a paginated, filtered and
   sorted collection with `Link` headers, one resource per order, a cancellation modelled as a
   resource, and problem details for every error, including invalid query parameters.

Do them in any order. Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| Resources and REST | Is this a resource or a procedure? PUT, PATCH or POST? How do I model "cancel"? |
| Naming, URL Design and Conventions | Path or query? Is this name consistent with the rest of the API? |
| Catalog Resources (kata) | Which status for create, delete, missing, wrong method? Where does `Location` point? |
| Errors as Problem Details | Which `type`? What goes in `detail`, what in an extension? What must a 500 hide? |
| Problem Responses (kata) | 404, 409 or 422? How do I report two invalid fields at once? |
| Pagination, Filtering and Sorting | Offset or cursor? Is my sort key unique? What are the default and the maximum? |

> [!TIP]
> If a quiz question surprises you, open the lesson it comes from again before retrying. The
> explanation after each answer says which idea it tests.
