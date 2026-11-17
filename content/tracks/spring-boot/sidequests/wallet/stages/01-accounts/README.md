Accounts first, stored with JPA. An account has an `id`, an `owner`, a `currency` and a
`balanceCents` that starts at 0.

| Request | Answer |
| --- | --- |
| `POST /accounts` `{"owner": "ada", "currency": "EUR"}` | `201`, `Location: .../accounts/{id}`, the account |
| `GET /accounts/{id}` | `200`, the account |
| `GET /accounts?owner=ada` | `200`, the accounts of that owner, by currency |
| `POST /accounts/{id}/deposits` `{"amountCents": 1500}` | `200`, the account with its new balance |
| `POST /accounts/{id}/withdrawals` `{"amountCents": 500}` | `200`, the account with its new balance |

```json
{"id": 1, "owner": "ada", "currency": "EUR", "balanceCents": 1500}
```

Rules:

- `owner` is not blank (at most 40 characters, stored stripped); `currency` is three capital
  letters (`EUR`, `USD`, ...). A bad body is `400` with the `errors` object.
- An owner has **at most one account per currency**: a second `ada`/`EUR` is `409`. Let the
  database guarantee it with a unique constraint, because two requests checking "does it
  exist?" at the same moment would both say no. When the constraint refuses the insert, the
  answer is still `409`, never `500`.
- `amountCents` is from 1 to 100000000 (`400` otherwise).
- A withdrawal never takes the balance below 0: `409` with the detail
  `account <id> has <balance> cents, needs <amount>`, and the balance does not change.
- An unknown account is `404` with the detail `account <id> not found`.

> Tip: an `@Entity` with `@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"owner",
> "currency"}))`, a `JpaRepository<Account, Long>` with `List<Account> findByOwnerOrderByCurrency(String
> owner)`, `@Transactional` on the service methods that change data, and an `@ExceptionHandler` for
> `DataIntegrityViolationException`. `saveAndFlush` makes the insert happen inside your method, where
> you can still catch its error.
