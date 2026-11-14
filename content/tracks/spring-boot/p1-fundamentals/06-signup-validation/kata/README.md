# Validated Sign-up

Members sign up with a name, an email and an age. `Member`, `EmailTakenException` and
`MemberNotFoundException` are written. `ApiErrors` already extends
`ResponseEntityExceptionHandler`, which answers Spring's own errors (malformed JSON, unknown
paths...) as Problem Details.

## 1. Constraints on `SignupRequest`

| Field | Rule |
| --- | --- |
| `name` | required, not blank, at most 40 characters |
| `email` | required, not blank, a valid email |
| `age` | required, at least 13 |

## 2. `MemberService`

- `register(request)`: stores a `Member` with the next id (from 1), the name **stripped** and
  the email **stripped and lowercased**. An email already registered (compared after
  normalising) throws `EmailTakenException`.
- `get(id)`: the member, or `MemberNotFoundException`.

## 3. `SignupController`

| Request | Answer |
| --- | --- |
| `POST /signups` (validated with `@Valid`) | `201 Created`, `Location: .../members/{id}`, the member |
| `GET /members/{id}` | `200`, the member |

## 4. `ApiErrors` (a `@RestControllerAdvice`)

Every error is a `ProblemDetail` (`application/problem+json`):

| Exception | Status | `title` | `detail` |
| --- | --- | --- | --- |
| `MethodArgumentNotValidException`: override `handleMethodArgumentNotValid` | 400 | `Invalid request` | `invalid request`, plus a property `errors`: field → first message |
| `EmailTakenException` | 409 | `Email already registered` | the exception's message |
| `MemberNotFoundException` | 404 | `Member not found` | the exception's message |

```json
POST /signups {"name": "", "email": "nope", "age": 10}
→ 400 {"title": "Invalid request", "status": 400, "detail": "invalid request",
       "errors": {"age": "must be greater than or equal to 13", "email": "...", "name": "..."}}
```

Emails are trimmed and lowercased only after validation, so `ADA@Example.com` is valid but an
email with a space in front is not.

> Tip: `jakarta.validation.constraints.*`, `ProblemDetail.forStatusAndDetail(status, detail)`,
> `problem.setTitle(...)`, `problem.setProperty("errors", map)`,
> `e.getBindingResult().getFieldErrors()`, and `handleExceptionInternal(e, problem, headers, status, request)`
> to answer from the override.
