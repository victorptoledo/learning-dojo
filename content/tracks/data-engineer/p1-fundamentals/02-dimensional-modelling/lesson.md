# Dimensional Modelling: Star Schemas, Grain, Facts and Dimensions

An application database is **normalised** so that each fact is written once and updates
are safe. Analysts need the opposite: few joins, names they understand, and fast
aggregates. Ralph Kimball's **dimensional modelling** gives a repeatable way to get there,
and it is still the backbone of most warehouses and of tools such as dbt and Power BI.

## Facts and dimensions

- A **fact table** records business **events** or measurements: an order line, a payment,
  a page view. It has foreign keys to dimensions and numeric **measures** (quantity,
  revenue). It is long and narrow, and it grows fast.
- A **dimension table** gives the **context** of the events: who, what, where, when. It
  has descriptive attributes (customer name, city, segment; product category) that people
  filter and group by. It is short and wide.

```text
                dim_date
                   |
dim_customer -- fact_sales -- dim_product
                   |
                dim_store
```

That shape is the **star schema**: one fact in the middle, one join to each dimension.

## The four steps

1. **Choose the business process**: sales, shipments, check-ins. One fact table per process.
2. **Declare the grain**: what exactly one row means, in one sentence: "one row per order
   line". This is the most important decision. Everything else must agree with it.
3. **Identify the dimensions**: what describes one row at that grain (date, customer,
   product, store).
4. **Identify the facts** (measures): the numbers true at that grain (quantity, unit price,
   revenue).

The **atomic** grain (the most detailed the source has) is usually best: you can always
aggregate up, never down. A fact table at "revenue per day per store" cannot answer "which
products sell together".

## Keys

- **Surrogate keys** (`customer_key`, an integer the warehouse assigns) identify a
  dimension row. They are not the source id (the **natural key**, `customer_id`), because
  one customer can have several rows over time (the type 2 history of the next phase) and
  several sources may use clashing ids.
- The **date dimension** has one row per day with ready-made attributes: year, quarter,
  month, week, day of week, holidays, fiscal periods. Its key is often the readable integer
  `20261115`. It turns "revenue by quarter on weekdays" into a join and a `WHERE`.
- A **degenerate dimension** is an id with no attributes of its own, kept in the fact table:
  the order number on an order-line fact.

## Measures and additivity

| Kind | Example | Can be summed across |
| --- | --- | --- |
| Additive | quantity, revenue | every dimension |
| Semi-additive | account balance, stock on hand | every dimension except time (sum balances across accounts, not across days: take the last or the average) |
| Non-additive | unit price, a ratio, a percentage | nothing: store the parts (revenue and quantity) and divide after aggregating |

`avg(unit_price)` over order lines is not the average price paid; `sum(revenue) /
sum(quantity)` is.

## Other fact table types

- **Periodic snapshot**: one row per entity per period (account balance at month end).
- **Accumulating snapshot**: one row per process instance, updated as it moves (order placed,
  paid, shipped, delivered dates in one row).
- **Factless fact**: events with no measure, only keys (a student attended a class);
  you count rows.

## Stars, snowflakes and wide tables

A **snowflake** normalises dimensions further (`dim_product` -> `dim_category`). It saves
a little space and costs extra joins and confusion; prefer stars. Modern column stores
also make **one big table** (the fact with dimension attributes already joined) cheap,
and it is a fine serving layer for one dashboard, but keep the star underneath as the
reusable model. Dimensions shared by several facts with the same meaning (one
`dim_customer` for sales and support tickets) are **conformed** dimensions: they make
results from different processes comparable.

## Summary

- Facts are events with measures; dimensions are the context people filter by.
- Process, grain, dimensions, facts: declare the grain first and keep it atomic.
- Use surrogate keys, a date dimension and conformed dimensions.
- Know which measures are additive; store parts of ratios.

## Further reading

- Ralph Kimball and Margy Ross, *The Data Warehouse Toolkit*, 3rd edition (Wiley), chapters 1 to 3
- Kimball Group, "Dimensional Modeling Techniques": <https://www.kimballgroup.com/data-warehouse-business-intelligence-resources/kimball-techniques/dimensional-modeling-techniques/>
- Microsoft Learn, "Understand star schema and the importance for Power BI": <https://learn.microsoft.com/en-us/power-bi/guidance/star-schema>
- PostgreSQL documentation, `generate_series` for calendars: <https://www.postgresql.org/docs/17/functions-srf.html>
