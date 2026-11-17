The last doors: guessing passwords, not knowing what happened, keeping a leaked password
forever, and browsers that do more than they should.

**Lockout.** The settings are already in `application.properties`:

```properties
secure.lockout.attempts=5
secure.lockout.duration=PT15M
```

- After `attempts` wrong passwords **in a row** for an existing user, that account is locked
  for `duration`. While it is locked, even the right password is `401`, by HTTP Basic and by
  the form login. When the time is over, the right password works again.
- A successful login starts the count again. Unknown usernames are `401` as always and lock
  nothing.
- Time comes from a `java.time.Clock` **bean** that you define; the tests replace it with one
  they move by hand. Read both settings from configuration: the tests use other values.

**Audit trail.** Record security events in the database:

| Type | When |
| --- | --- |
| `LOGIN_FAILURE` | a wrong password for an existing user |
| `ACCOUNT_LOCKED` | an account gets locked |
| `PASSWORD_CHANGED` | a user changes their password |

| Request | Who | Answer |
| --- | --- | --- |
| `GET /api/admin/audit?username=ada` | an admin | `200`, the events of that user `{"type", "username", "at"}`, newest first |
| `GET /api/admin/audit` | an admin | `200`, the last 100 events of everyone, newest first |

**Password change.**

| Request | Who | Answer |
| --- | --- | --- |
| `PUT /api/me/password` `{"currentPassword", "newPassword"}` | a user | `204`; only the new password works from now on |

A wrong `currentPassword` is `400` with the error on `currentPassword`; a `newPassword` that
breaks the sign-up rules is `400` with the error on `newPassword`.

**Security headers** on every response, errors included:

| Header | Value |
| --- | --- |
| `Content-Security-Policy` | `default-src 'none'; frame-ancestors 'none'` |
| `X-Content-Type-Options` | `nosniff` |
| `X-Frame-Options` | `DENY` |
| `Referrer-Policy` | `no-referrer` |
| `Cache-Control` | contains `no-store` |

> Tip: Spring Security publishes `AuthenticationFailureBadCredentialsEvent` and
> `AuthenticationSuccessEvent`; an `@EventListener` on a `@Transactional` service method can
> count the failures. Return the lock through `UserDetails` (`accountLocked(...)`) and Spring
> refuses the login before it even checks the password. `.headers(headers -> headers
> .contentSecurityPolicy(...).referrerPolicy(...))` adds the headers.
