A browser app does not send a password on every request: it logs in once and the server
remembers it in a **session** (a cookie). Cookies go with every request the browser makes,
even one that another site triggers, so a session needs **CSRF** protection: every request
that changes something must also carry a token that only your own page can read.

| Request | Who | Answer |
| --- | --- | --- |
| `GET /api/csrf` | anyone | `200`, `{"headerName": "X-CSRF-TOKEN", "token": "..."}` for the current session |
| `POST /api/login` (form fields `username`, `password`) | anyone | `200`, the user (`{"username", "roles"}`), and the session is logged in |
| `POST /api/logout` | anyone | `204`; the session is logged out |

Rules:

- The login is a **form post** (`application/x-www-form-urlencoded`), not JSON. Wrong
  credentials are `401` and the session stays anonymous.
- After the login, the session works like HTTP Basic did: `GET /api/me` answers for that user.
  HTTP Basic keeps working too.
- **CSRF is on** for every `POST`, `PUT`, `PATCH` and `DELETE`, login, sign-up and logout
  included: without the token in the `X-CSRF-TOKEN` header, or with a wrong one, the answer
  is `403`. `GET` requests never need it.
- **Session fixation:** a login gets a **new session id**, so an id someone planted before the
  login is useless after it. The CSRF token changes at the login too: ask `/api/csrf` again
  after logging in.
- A request without a login is `401`, never a redirect to a login page (this is an API).

A client does: `GET /api/csrf` → `POST /api/login` with the token → `GET /api/csrf` again →
any `POST` with the new token.

> Tip: remove the stateless policy; `.formLogin(form -> form.loginProcessingUrl("/api/login")
> .successHandler(...).failureHandler(...))` with handlers that write JSON or a status;
> `.logout(logout -> logout.logoutUrl("/api/logout").logoutSuccessHandler(new
> HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT)))`;
> `.exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))`;
> a controller method with a `CsrfToken` parameter returns the token. The default CSRF setup
> keeps the token in the session and reads the `X-CSRF-TOKEN` header.
