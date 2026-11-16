# Checkpoint 2: Evolving Customers

Last week the team changed the customer model (split names, several emails, a new `SUSPENDED`
status, a country) and served it straight from `/customers`. The mobile app, which still
speaks the first version of the API and cannot be updated overnight, broke.
`src/test/java/dojo/LegacyClientContractTest.java` is that app's **consumer contract**: it fails
today. Make it pass again **without** giving up the new model, and give new clients a second
version.

`Customer`, `CustomerStatus`, `CustomerStore` (seeded with customers 1 to 3) and `ApiProblems`
are written. Rework `CustomerController` and add what you need. Do not change
`LegacyClientContractTest`.

## Version 1: `/customers` (frozen, deprecated)

| Request | Answer |
| --- | --- |
| `GET /customers/{id}` | `200`, `{"id", "name", "email", "phone", "status"}` |
| `POST /customers` with `{"name", "email", "phone"}` (name and email required, email valid) | `201`, `Location: /customers/{id}`, the same shape |

Computed from the new model:

- `name` is `givenName + " " + familyName` (without trailing space when the family name is
  empty). On sign-up, the name is split at the first space.
- `email` is the **first** of `emails`; a v1 sign-up stores a list of one.
- `status` is `ACTIVE` or `INACTIVE` only: v1 clients have a closed enum, so `SUSPENDED` is
  `INACTIVE` for them.
- A v1 sign-up has no country: store `null`. Unknown request fields are ignored.

Every v1 answer (`GET` and `POST`) announces the end of version 1:

```text
Deprecation: @1793491200                       (RFC 9745: deprecated since 2026-11-01T00:00:00Z)
Sunset: Tue, 01 Jun 2027 00:00:00 GMT          (RFC 8594: may stop answering after this date)
Link: </v2/customers/{id}>; rel="successor-version",
      <https://api.dojo.example/docs/migrate-to-v2>; rel="deprecation"
```

## Version 2: `/v2/customers`

| Request | Answer |
| --- | --- |
| `GET /v2/customers/{id}` | `200`, `{"id", "givenName", "familyName", "emails", "phoneNumber", "status", "country"}` |
| `POST /v2/customers` with `{"givenName", "familyName", "emails", "phoneNumber", "country"}` | `201`, `Location: /v2/customers/{id}`, the v2 shape |

Validation (`400`, a problem detail): `givenName` not blank, `familyName` present (may be
empty), `emails` not empty and each a valid email, `country` required and two uppercase
letters. No deprecation headers on v2. Both versions are views of **the same customers**: a
customer created through one is readable through the other.

## Caching per representation

Both versions send an `ETag` on `GET` and `POST`, and answer `If-None-Match` with `304`. The
two representations of one customer are different bytes, so their ETags must differ: a v1
ETag sent to v2 is a `200`. (`Customer.version` tells you when a customer changes.)

> Tip: keep one representation record per version (`CustomerV1`, `CustomerV2`) with a static
> `of(Customer)`; the domain stays single. A `ResponseEntity` with `.eTag(...)` on a `GET`
> already gets `304` handling from Spring MVC.
