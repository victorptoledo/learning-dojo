Users now create **projects**, share them with other users, and an administrator looks after
everything. Most of this stage is not about logging in but about **who may do what**, and
about answering in a way that does not leak what exists.

**The first administrator** comes from configuration (already in `application.properties`):

```properties
secure.admin.username=root
secure.admin.password=${SECURE_ADMIN_PASSWORD:root-password-for-dev}
```

At startup, create that user with roles `ADMIN` and `USER` when it does not exist yet, its
password hashed like everyone else's. Read the values from configuration: the tests start the
application with another username and password.

**Projects** (`{"id", "name", "owner", "members"}`, `members` sorted by username):

| Request | Who | Answer |
| --- | --- | --- |
| `POST /api/projects` `{"name": "Apollo"}` | a user | `201`, `Location: .../api/projects/{id}`, the project, owned by that user |
| `GET /api/projects` | a user | `200`, the projects the user owns or is a member of, by id |
| `GET /api/projects/{id}` | owner, member or admin | `200`, the project |
| `PUT /api/projects/{id}/members/{username}` | owner or admin | `200`, the project with that member |
| `DELETE /api/projects/{id}/members/{username}` | owner or admin | `200`, the project without that member |
| `DELETE /api/projects/{id}` | owner or admin | `204` |

- The name is 1 to 60 characters, not blank (`400`).
- Someone who is **not** owner, member or admin gets `404` (`project <id> not found`) for
  everything about the project, exactly as for a project that does not exist.
- A **member** sees the project but cannot change it: `403` for members and deletion.
- Adding an unknown user as a member is `404` (`user <username> not found`).

**Administration** (`/api/admin/**` is for role `ADMIN` only; others get `403`):

| Request | Answer |
| --- | --- |
| `GET /api/admin/users` | `200`, every user `{"username", "roles"}`, by username |
| `PUT /api/admin/users/{username}/roles` `{"roles": ["USER", "ADMIN"]}` | `200`, the user with those roles |

- Roles are a non-empty list of `USER` and `ADMIN` (`400` otherwise); an unknown user is `404`.
- An administrator cannot take `ADMIN` away from themselves (`409`): someone must stay able
  to administer.
- A new role counts from the next request of that user.

> Tip: `@ConfigurationProperties("secure")` for the settings, an `ApplicationRunner` for the
> first administrator, `@EnableMethodSecurity` with `@PreAuthorize("hasRole('ADMIN')")` on the
> admin endpoints, and `.requestMatchers("/api/admin/**").hasRole("ADMIN")` as a second fence.
> Decide owner, member or admin in one service method that every endpoint calls.
