# Secured Notes

Three users exist (see `UsersConfig`): `ada` / `ada-pass` and `linus` / `linus-pass` with role
`USER`, and `root` / `root-pass` with roles `USER` and `ADMIN`. You write the security rules,
the service and the controller.

## Security (`SecurityConfig`)

A `SecurityFilterChain` bean for a stateless JSON API:

- `GET /notes/count` is **public**;
- `/admin/**` needs role `ADMIN` (a logged-in user without it gets **403**);
- everything else needs a login (no or wrong credentials: **401**);
- log in with **HTTP Basic**; no sessions; CSRF off (credentials come in a header on every request).

## Endpoints

| Request | Who | Answer |
| --- | --- | --- |
| `GET /notes/count` | anyone | `200` `{"count": N}` (all notes) |
| `POST /notes` `{"text": "..."}` (validated) | a user | `201`, `Location: .../notes/{id}`, the note owned by that user |
| `GET /notes` | a user | `200`, that user's notes, by id |
| `GET /notes/{id}` | the owner, or an admin | `200`; anyone else gets `404` |
| `DELETE /notes/{id}` | the owner, or an admin | `204`; anyone else gets `404` |
| `GET /admin/notes` | an admin | `200`, every note |

A note of another user answers **404** (`NoteNotFoundException`), not 403, so its id is not
revealed.

> Tip: `http.authorizeHttpRequests(auth -> auth.requestMatchers(...).permitAll() ...)`,
> `.httpBasic(Customizer.withDefaults())`, `.csrf(csrf -> csrf.disable())`,
> `SessionCreationPolicy.STATELESS`; in the controller a `Principal` or `Authentication`
> parameter gives the user's name and authorities (`ROLE_ADMIN`).
