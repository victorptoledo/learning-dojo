# Data Quality and Testing

A pipeline that runs green and publishes wrong numbers is worse than one that fails: people
make decisions on it. Data quality is not a one-off cleaning job; it is **checks that run
on every batch**, in the right places, with clear rules about what stops publication.

## Dimensions of quality

| Dimension | Question | Example check |
| --- | --- | --- |
| **Completeness** | Is everything there? | required columns not null; row count within the usual range |
| **Uniqueness** | Is each thing there once? | `payment_id` unique; one current version per customer |
| **Validity** | Do values follow the rules? | `currency` in an allowed list; `amount >= 0`; dates parse |
| **Consistency** | Do sources agree with each other? | every `customer_id` exists in `dim_customer`; totals match the source |
| **Accuracy** | Does it match reality? | reconciliation with finance's ledger for the month |
| **Timeliness** | Is it fresh enough? | newest `paid_at` less than 2 hours old |

## Tests on data

The most common checks are few and cheap, and tools ship them ready-made: dbt's generic
tests (`not_null`, `unique`, `accepted_values`, `relationships`), Great Expectations and Soda.
In SQL, every check is a query that returns the **failing rows**; zero rows means a pass:

```sql
-- unique: any payment_id more than once
SELECT payment_id, count(*) FROM stg_payments GROUP BY payment_id HAVING count(*) > 1;

-- relationships: orphans
SELECT p.* FROM stg_payments AS p
LEFT JOIN dim_customer AS c ON c.customer_id = p.customer_id
WHERE c.customer_id IS NULL;
```

Returning the failing rows, not only a count, makes failures easy to investigate. Add
**volume** checks (today's rows within, say, 50 to 150 percent of the last 7 days) and
**freshness** checks: they catch the silent failures, such as an extract that returned
nothing because a token expired.

## Where checks run

```text
source -> raw -> [checks on input] -> staging -> marts -> [checks on output] -> publish
```

- **On input** (after landing): schema matches, required fields present, volume plausible.
  Catch problems before they spread.
- **On output** (before publishing): uniqueness at the declared grain, relationships,
  business rules, reconciliation of totals.
- **On the code**: unit tests of transformation functions or SQL models on small, hand-made
  inputs, in CI, before anything runs on real data.

## Severities and quality gates

Not every failure should stop the pipeline. Give each check a **severity**:

- **error**: the data must not be published (duplicated payments, missing keys). The run
  stops at a **quality gate**; consumers keep yesterday's good data instead of today's bad
  data, and someone is alerted.
- **warn**: worth a look, not worth blocking (a handful of unknown country codes). Log it,
  count it, alert if it grows.

A useful pattern is **write-audit-publish**: write the new data to a hidden table or
partition, audit it with the checks, and only then publish it (swap, rename, or move a
pointer). Failed audits never reach consumers.

## Data contracts

Many quality problems start upstream: a team renames a column or starts sending amounts in
cents. A **data contract** is an agreement between the producer and the consumers of a
dataset: the schema, the meaning of each field, the quality guarantees (unique keys,
freshness), and how changes are announced and versioned. Contracts work best when they are
**checked automatically** in the producer's CI (a schema registry for events, a schema test
for tables), so a breaking change fails their build, not your pipeline at 3:00.

## Unit tests for pipelines

Test transformation logic like any code, with tiny inputs you understand:

```python
def test_refunds_are_negative_revenue():
    rows = [{"payment_id": 1, "amount": 50, "status": "refunded"}]
    assert revenue(rows) == -50
```

Fixtures for edge cases (nulls, duplicates, time zones, empty inputs) pay for themselves.
The Modern Python track covers pytest itself.

## Summary

- Quality has dimensions: completeness, uniqueness, validity, consistency, accuracy, timeliness.
- A check is a query for failing rows; volume and freshness catch silent failures.
- Check inputs after landing and outputs before publishing; unit test the code in CI.
- Severities decide what blocks; a quality gate keeps bad data away from consumers.
- Data contracts move checks upstream, into the producer's CI.

## Further reading

- dbt documentation, data tests: <https://docs.getdbt.com/docs/build/data-tests>
- Great Expectations documentation, expectations: <https://docs.greatexpectations.io/>
- Soda documentation, SodaCL checks: <https://docs.soda.io/>
- Andrew Jones, *Driving Data Quality with Data Contracts* (Packt, 2023)
- The Open Data Contract Standard (Bitol): <https://bitol-io.github.io/open-data-contract-standard/>
