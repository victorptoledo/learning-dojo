# Problem Responses

A small bank API answers every failure with `200 OK` and `{"error": "..."}`, accepts any
input, and would happily send a stack trace message to the world. Give it the right status
codes and make **every** error an RFC 9457 problem detail (`application/problem+json`).

`Account`, `Withdrawal`, `WithdrawalRequest` (with its constraints), `BankExceptions` and `Bank`
(in memory: `acc-1` Ada 100.00, `acc-2` Linus 5.00, `acc-3` Grace 20.00 and **frozen**) are
written. Change `AccountController` and write `ApiProblems`.

## Successful requests

| Request | Answer |
| --- | --- |
| `GET /accounts/{id}` | `200`, the account |
| `POST /accounts/{id}/withdrawals` (validated) | `201 Created`, `Location: .../accounts/{id}/withdrawals/{withdrawalId}`, the withdrawal |

## Errors

Every problem has `type`, `title`, `status`, `detail` and `instance` (the request path).
Types start with `https://api.dojo.example/problems/` (the constant `ApiProblems.TYPES`).

| Cause | Status | `type` suffix | `title` | `detail` | Extension members |
| --- | --- | --- | --- | --- | --- |
| `AccountNotFoundException` | 404 | `account-not-found` | `Account not found` | the message | |
| `AccountFrozenException` | 409 | `account-frozen` | `Account frozen` | the message | |
| `InsufficientFundsException` | 422 | `insufficient-funds` | `Insufficient funds` | the message | `balance`, `requested` (numbers, cents) |
| invalid fields (`@Valid`) | 400 | `validation-error` | `Invalid request` | `the request has N invalid field(s)` | `errors`: a list of `{"pointer": "#/field", "detail": "..."}`, sorted by pointer |
| malformed JSON, unknown path | 400, 404 | (Spring's defaults) | | | |
| anything else | 500 | (`about:blank`) | `Internal Server Error` | `an unexpected error occurred` | |

```json
POST /accounts/acc-2/withdrawals {"amountCents": 900, "reference": "bike"}
→ 422 Content-Type: application/problem+json
{
  "type": "https://api.dojo.example/problems/insufficient-funds",
  "title": "Insufficient funds",
  "status": 422,
  "detail": "balance 500 is less than the 900 requested",
  "instance": "/accounts/acc-2/withdrawals",
  "balance": 500,
  "requested": 900
}
```

Use the singular for one error: `the request has 1 invalid field`. A `500` must not reveal
anything about the bug: no exception class, no message, no host names. Log it on the server
instead.

> Tip: `ProblemDetail.forStatusAndDetail(status, detail)`, `setType`, `setTitle`,
> `setInstance`, `setProperty`; an `@ExceptionHandler` method may take the
> `HttpServletRequest`. Override `handleMethodArgumentNotValid` (do not add a second handler for
> `MethodArgumentNotValidException`) and answer with `handleExceptionInternal(...)`.
