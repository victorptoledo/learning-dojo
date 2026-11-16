# A Session API for a Single-Page App

The team's new SPA runs at `https://app.example.com` and calls this Spring Boot API with
`fetch(url, { credentials: "include" })`. It must not keep tokens in `localStorage`, where any
injected script could read them. Instead, the API keeps a **server-side session** in an
`HttpOnly` cookie, protects every state-changing request against CSRF with a token the SPA reads
from a cookie and sends back in a header, and lets only the SPA's origin call it with
credentials.

Two users exist (`UsersConfig`, which also gives you an `AuthenticationManager`):
`ada` / `ada-pass` and `linus` / `linus-pass`, both with role `USER`. `Profiles` keeps each
user's display name.

## Security (`SecurityConfig`)

A `SecurityFilterChain` bean:

- **CSRF on**, SPA style: `csrf(csrf -> csrf.spa())` stores the token in a cookie `XSRF-TOKEN`
  that JavaScript can read, and expects it back in the `X-XSRF-TOKEN` header. A `POST`, `PUT` or
  `DELETE` without it is a `403`. Login is not exempt (login CSRF is a real attack);
- `POST /api/login` and `GET /api/csrf` are public; every other `/api/**` needs a logged-in user;
  anything else is denied;
- an anonymous request to a protected endpoint is a **`401`** with the JSON
  `{"error": "unauthenticated"}`, never a redirect to a login page (an SPA cannot follow one);
- no form login, no HTTP Basic, no default logout page;
- **CORS** through a `CorsConfigurationSource` bean: only the origin `https://app.example.com`,
  methods `GET`, `POST`, `PUT`, `DELETE`, request headers `Content-Type` and `X-XSRF-TOKEN`,
  **with credentials** (so `Access-Control-Allow-Origin` can never be `*`). A preflight from any
  other origin is refused (`403`).

## Endpoints (`AuthController`)

| Request | Answer |
| --- | --- |
| `GET /api/csrf` | `200` `{"headerName": "X-XSRF-TOKEN", "token": "..."}`; it also sets the `XSRF-TOKEN` cookie, so the SPA calls it once at start-up |
| `POST /api/login` `{"username": "...", "password": "..."}` | `200` `{"username": "ada"}` and the user is now logged in for this session; wrong credentials: `401` `{"error": "bad credentials"}` |
| `GET /api/me` | `200` `{"username": "ada", "displayName": "...", "roles": ["USER"]}` (the display name defaults to the username) |
| `PUT /api/me` `{"displayName": "..."}` | `200`, the same body with the new name (trimmed); a blank one is a `400` |
| `POST /api/logout` | `204`, the session is invalidated: the next `GET /api/me` with it is a `401` |

To log in by hand, authenticate with the `AuthenticationManager`, put the result in a new
`SecurityContext`, and **save** it with an `HttpSessionSecurityContextRepository`: Spring Security
does not save contexts you create yourself.

## The session cookie (`application.properties`)

Set `server.servlet.session.cookie.http-only=true`, `server.servlet.session.cookie.secure=true`
and `server.servlet.session.cookie.same-site=lax`.

## How the kata is checked

The tests drive the API with MockMvc: they log in, keep the session, read the `XSRF-TOKEN`
cookie like the SPA does, and send preflight requests from both origins. There are 14 tests: 2 in
`src/test/java/dojo/SessionApiTest.java` and 12 hidden ones.
