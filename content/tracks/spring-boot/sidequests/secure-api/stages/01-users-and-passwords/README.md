Users first. Anyone can sign up; then they log in with HTTP Basic.

| Request | Who | Answer |
| --- | --- | --- |
| `POST /api/signup` `{"username": "ada", "password": "..."}` | anyone | `201`, the user `{"username": "ada", "roles": ["USER"]}` |
| `GET /api/me` | a user | `200`, the same shape for the user who is logged in |

Rules:

- `username` is 3 to 20 characters of `a-z`, `0-9` and `_`. `password` is 12 to 72
  characters and does not contain the username (ignoring case). A bad body is `400` with the
  `errors` object (`username`, `password`).
- Usernames are unique: a second `ada` is `409`, also when two sign-ups race (a unique
  constraint in the database).
- Every other `/api` path needs a login: `401` without one, with a wrong password or with an
  unknown user.

**The password is never stored.** Store only a slow, salted hash: expose a `PasswordEncoder`
bean (BCrypt, or `PasswordEncoderFactories.createDelegatingPasswordEncoder()`, which writes
`{bcrypt}...`) and a `UserDetailsService` bean that loads users from the database. The tests
read a user back through it and check that the stored value is a BCrypt hash that matches the
password, and that two users with the same password get different hashes.

Keep the API stateless for now (`SessionCreationPolicy.STATELESS`) and CSRF off: the next
stage adds sessions and turns CSRF on. The tests already send a CSRF token on every `POST`, so
they keep passing then.

> Tip: an `@Entity` user with a unique `username`, the hash and its roles; a
> `SecurityFilterChain` bean with `authorizeHttpRequests(...)`, `.httpBasic(withDefaults())`;
> `User.withUsername(...).password(hash).roles(...)` builds the `UserDetails`; an
> `Authentication` parameter in a controller method gives the logged-in user.
