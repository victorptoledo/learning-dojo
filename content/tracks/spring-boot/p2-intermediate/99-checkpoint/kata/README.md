# Checkpoint 2: Orders and Stock

A shop places orders against its stock. The entities (`Product`, `ShopOrder`, `OrderLine`), the
records, `ShopException` and the error handling are written. `Product.reserve` refuses when
there is not enough stock; `ShopOrder.cancel` refuses a second cancel.

You write the repositories, `OrderService` and `OrderController`.

## Endpoints

| Request | Answer |
| --- | --- |
| `POST /orders` `{"lines": [{"sku": "tea", "quantity": 3}]}` (validated) | `201`, `Location: .../orders/{id}`, the `OrderView` |
| `GET /orders/{id}` | `200`, the order with its lines |
| `POST /orders/{id}/cancel` | `200`, the cancelled order |

## Rules

- `place` is **all or nothing**. For each line, find the product by sku (unknown →
  `ShopException.unknownProduct`, 404), `reserve` the quantity (not enough →
  `outOfStock`, 409) and add the line with the product's current price. If any line fails, the
  stock of **every** line stays as it was.
- `get` reads the order **with its lines in one query** (a `left join fetch`): the
  application runs with `spring.jpa.open-in-view=false`, so lazy lines are not loaded after the
  service returns.
- `cancel` cancels the order (`alreadyCancelled`, 409, the second time) and **releases** the
  stock of each line. Unknown orders are `orderNotFound` (404).
- Changes to products loaded inside the transaction are saved at commit; you do not need to
  call `save` on them.

> Tip: `Optional<Product> findBySku(String sku)`,
> `@Query("select o from ShopOrder o left join fetch o.lines where o.id = :id")`,
> `@Transactional` and `@Transactional(readOnly = true)`, and `OrderView.of(order)` inside the
> transaction.
