# Naming, URL Design and Representation Conventions

Names are the most visible part of an API and the hardest to change. A developer reads them
hundreds of times; a style guide that settles them once saves every review from the same
debates. This lesson collects the conventions most public guidelines agree on.

## URLs

| Rule | Good | Avoid |
| --- | --- | --- |
| Plural nouns for collections | `/orders`, `/orders/42` | `/order/42`, `/getOrder` |
| Lowercase, words joined with hyphens (kebab-case) | `/line-items` | `/lineItems`, `/line_items` |
| No verbs: the method is the verb | `DELETE /orders/42` | `POST /orders/42/delete` |
| No file extensions; use `Accept` | `/reports/7` + `Accept: text/csv` | `/reports/7.csv` (acceptable for downloads, not for resources) |
| No trailing slash | `/orders` | `/orders/` (pick one, redirect the other) |
| Stable, opaque ids | `/orders/ord_8f2kq` | `/orders/42?db=eu-2` |
| Short nesting | `/orders/42/lines` | five levels of parents |

Path segments name **resources**; the **query string** narrows, shapes or pages a collection:
`/orders?status=PAID&sort=-createdAt&limit=20`. A rule of thumb: if removing the parameter
still names the same thing, it belongs in the query.

### Identifiers

- Prefer opaque string ids. Sequential numbers leak volume ("we are order 1042, they had ten
  orders today") and invite enumeration attacks; a prefix such as `ord_`, `cus_` makes ids
  self-describing in logs and support tickets (Stripe does this).
- Never change the meaning of an id, and never reuse one.
- Accept ids as strings even if they look numeric today.

## Fields and JSON

| Topic | Convention |
| --- | --- |
| Case | **camelCase** for JSON members is the most common (Google, Microsoft); snake_case is fine (Stripe, Zalando) if it is used **everywhere** |
| Booleans | positive and prefixed: `isActive`, `hasChildren`, never `notDisabled` |
| Dates and times | RFC 3339 strings in UTC: `"2026-03-01T10:00:00Z"`; date-only `"2026-03-01"`; durations ISO 8601 `"PT30S"` |
| Money | an integer in minor units plus a currency (`{"amount": 2999, "currency": "EUR"}`) or a decimal **string**; never a binary float |
| Enums | UPPER_SNAKE or lowercase strings, documented as **open**: clients must tolerate new values |
| Nulls | decide whether absent and `null` mean the same; say so in the guide |
| Collections | wrap them in an object (`{"items": [...], "nextCursor": ...}`) so you can add metadata later; a top-level array cannot grow |
| Units | in the name when not obvious: `timeoutSeconds`, `sizeBytes`, `priceCents` |

## Consistency of names across the API

The same concept must have the same name everywhere: if one endpoint says `customerId`,
another must not say `clientId` or `customer_id`. Keep a **glossary** next to the style guide.
Avoid internal jargon and abbreviations (`qty`, `addr`, `cust`): the consumer does not share
your team's vocabulary.

## Query parameter conventions

| Purpose | Common names |
| --- | --- |
| Pagination | `limit`, `cursor` (or `pageSize`, `pageToken` in Google style); `page` and `size` for offset |
| Sorting | `sort=-createdAt,name` (a leading `-` for descending) or `orderBy=createdAt desc` |
| Filtering | one parameter per field (`status=PAID`), or a filter expression (`filter=status="PAID"`, AIP-160) |
| Sparse fields | `fields=id,status,total` |
| Expansion | `expand=customer` to embed a related resource |

Lesson 6 of this phase goes deeper into pagination, filtering and sorting.

## Further reading

- Google AIP-122, "Resource names", and AIP-140, "Field names": <https://google.aip.dev/122>, <https://google.aip.dev/140>
- Zalando RESTful API Guidelines, "REST Basics: JSON payload" and "URLs": <https://opensource.zalando.com/restful-api-guidelines/#json-guidelines>
- Microsoft REST API Guidelines, "Considerations for service design": <https://github.com/microsoft/api-guidelines/blob/vNext/azure/ConsiderationsForServiceDesign.md>
- RFC 3339, "Date and Time on the Internet: Timestamps": <https://www.rfc-editor.org/rfc/rfc3339>
- RFC 3986, "Uniform Resource Identifier (URI): Generic Syntax": <https://www.rfc-editor.org/rfc/rfc3986>
- Stripe API reference, "Object IDs" and expanding responses: <https://docs.stripe.com/api/expanding_objects>
