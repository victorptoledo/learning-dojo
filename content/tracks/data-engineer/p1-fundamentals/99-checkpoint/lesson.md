# Checkpoint 1: Fundamentals

This checkpoint closes the **Fundamentals** phase. It has two parts, and you need both:

1. **The quiz** (press **Q**): eight questions over the whole phase. You need 7 right.
2. **The integration kata** (press **Enter**): a small sales warehouse on SQLite, loaded
   from a CSV of customers and a JSON-lines file of orders, with a star schema, reasons for
   every rejected record and loads you can run twice.

Do them in any order. Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| ETL vs ELT | Why keep raw data unchanged? What belongs in staging and what in marts? |
| Dimensional modelling | What is the grain? Why surrogate keys? Which measures can be summed across time? |
| File formats | Why does CSV lose types? Why read JSON decimals as `Decimal`? When Avro, when Parquet? |
| Incremental loads | What makes a load idempotent? What should the watermark store? What can a timestamp watermark not see? |

> [!TIP]
> If a quiz question surprises you, open the lesson it comes from again before retrying. The
> explanation after each answer says which idea it tests.
