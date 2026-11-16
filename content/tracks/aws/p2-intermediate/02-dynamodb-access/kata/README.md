# DynamoDB Access Patterns with boto3

Finish the five methods of `OrderRepository` in `orders.py`. The table `Orders` (created by
`tests/conftest.py` in moto) has the key `pk` (hash) and `sk` (range), and a global
secondary index `by-status` on `gsi1pk` (hash) and `gsi1sk` (range).

| Attribute | Value |
| --- | --- |
| `pk` | `CUSTOMER#<customer_id>` |
| `sk` | `ORDER#<order_id>` (order ids sort by time, such as `20261115-0042`) |
| `gsi1pk` | `STATUS#<status>` |
| `gsi1sk` | `<order_id>` |
| the order's own fields | `customer_id`, `order_id`, `status`, `total` (a `Decimal`), `items` (a list of strings) |

| Method | Rule |
| --- | --- |
| `put_order(order)` | stores the order with the four key attributes; raises `OrderExists` if the same customer already has that order id (use a condition, not a read first) |
| `get_order(customer_id, order_id)` | the order as a dict **without** the key attributes, or `None` |
| `orders_for_customer(customer_id, page_size=25)` | every order of the customer, newest order id first; `page_size` is the query's `Limit`, so follow `LastEvaluatedKey` until the last page |
| `orders_by_status(status)` | every order in that status, oldest first, from the `by-status` index |
| `mark_shipped(customer_id, order_id)` | sets `status` to `SHIPPED` (and `gsi1pk` with it) only if the order exists and is `PLACED`; otherwise raises `InvalidTransition` |

Never `scan`. `to_item` and `from_item` at the bottom of the module convert between Python
values and DynamoDB's typed JSON.

How the check works: pytest runs the tests against moto's in-memory DynamoDB, and ruff
checks `orders.py` for real mistakes (unused imports, undefined names).

```powershell
uv run --with boto3 --with "moto[dynamodb]" --with pytest pytest   # optional, on your machine
```

There are 14 tests: 1 in `tests/test_visible.py`, 12 hidden ones and the ruff check.
