# Checkpoint 1: Product Catalog

A product catalog whose limits come from configuration. `Product`, `ProductPage`, `PriceQuote`,
`DiscountRequest` and `CatalogException` are written.

## 1. Configuration

`application.properties` already has:

```properties
catalog.currency=EUR
catalog.max-page-size=20
catalog.max-discount-percent=50
```

Turn `CatalogProperties` into a validated `@ConfigurationProperties("catalog")` record
(`currency` not blank, `maxPageSize` 1 to 100, `maxDiscountPercent` 0 to 90), register it
(add `@ConfigurationPropertiesScan` to `DojoApplication`), and **inject it** where it is needed.
Never hard-code the values: a grading test runs with other settings.

## 2. Validation of `ProductRequest`

| Field | Rule |
| --- | --- |
| `sku` | required, three capital letters, a dash, three digits (`TEA-001`) |
| `name` | not blank |
| `priceCents` | required, positive |
| `category` | not blank |

## 3. `ProductService` (in memory, sorted by sku)

- `add`: stores the product with `name` stripped and `category` stripped and **lowercased**;
  an existing sku → `CatalogException.duplicate(sku)`.
- `get`: the product or `CatalogException.notFound(sku)`.
- `list(category, page, size)`: the products of that category (all when `null`; the category is
  compared stripped and lowercased), sorted by sku, page `page` (from 0) of `size` items, with
  `total` = how many match and the configured `currency`. A `size` above `maxPageSize` →
  `CatalogException.pageTooLarge(size, max)`.
- `discount(sku, percent)`: a `PriceQuote` with `discountedCents = priceCents * (100 - percent) / 100`
  (rounded down); `percent` above `maxDiscountPercent` → `CatalogException.discountTooHigh(percent, max)`.

## 4. `ProductController`

| Request | Answer |
| --- | --- |
| `POST /products` (validated) | `201`, `Location: .../products/{sku}`, the product |
| `GET /products/{sku}` | `200`, the product |
| `GET /products?category=&page=0&size=10` | `200`, a `ProductPage`; `page` ≥ 0 and `size` ≥ 1 or `400` |
| `POST /products/{sku}/discount` `{"percent": 15}` (validated) | `200`, a `PriceQuote` |

## 5. `ApiErrors`

A `CatalogException` becomes a `ProblemDetail` with its `status()` and its message as
`detail`. Validation errors are `400` with an `errors` property (field → first message), as in
the sign-up kata.

> This kata reviews the whole phase: a validated properties record, constructor injection,
> controller mappings and parameters, Bean Validation on bodies and parameters, and Problem Details.
