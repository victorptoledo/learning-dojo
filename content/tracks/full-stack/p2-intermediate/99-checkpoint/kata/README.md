# Checkpoint 2: a Comments Feature from Database to Page

Articles get comments. The product spec, from the page down:

- under each article the page shows the newest comments first, ten at a time, with a
  **"Show more"** button that appears only while older comments exist;
- logged-in readers post a comment from a form; when it is refused, the message appears **under
  the form field**, so errors must say which field they belong to;
- a comment shows a delete button to its author and to admins, and only to them.

The client is the mobile app and the SPA's token flow, which send credentials in a header on
every request (HTTP Basic here, to keep the kata small), so `SecurityConfig` (written for you)
is stateless with CSRF off. Three users exist (`UsersConfig`): `ada` and `linus` (`USER`) and
`root` (`USER`, `ADMIN`); the passwords are `<name>-pass`.

`Comment` (the JPA entity) is written for you. You write `CommentRepository`, `CommentController`
and the validation answer in `ApiErrors`.

## Endpoints

| Request | Who | Answer |
| --- | --- | --- |
| `GET /api/articles/{slug}/comments?limit=N&cursor=C` | anyone | `200` `{"items": [...], "nextCursor": "..." or null}` |
| `POST /api/articles/{slug}/comments` `{"body": "..."}` | a user | `201`, `Location: /api/comments/{id}`, the item |
| `DELETE /api/comments/{id}` | its author or an admin | `204`; another user: `403`; unknown id: `404` |

### The list

- Comments of that article only, **newest first** (highest `id` first).
- `limit` defaults to `10`; outside `1..50` it is a `400`.
- `nextCursor` is an **opaque** string: the base64url (no padding) of the id of the last item,
  when older comments exist, and `null` on the last page. A request with `cursor` returns the
  comments older than it; a cursor that does not decode to a number is a `400`. Clients never
  build cursors themselves.
- Each item: `{"id", "author", "body", "createdAt", "canDelete"}`. `createdAt` is the ISO-8601
  instant; `canDelete` is `true` when the viewer may delete it (its author, or an admin) and
  `false` for anonymous readers. The page uses it to show the button; the server still checks
  on `DELETE`.

### Posting

`body` must not be blank and has at most 500 characters; it is stored trimmed. A refused comment
is a `400` problem detail (`application/problem+json`) with an `errors` object from field name to
message, for example `{"status": 400, "errors": {"body": "must not be blank"}, ...}`, so the page
can put each message under its field. The author is the logged-in user, never a field of the
request.

## How the kata is checked

MockMvc tests post, list and delete as each user, follow cursors page by page and check the
answers field by field. There are 13 tests: 2 in `src/test/java/dojo/CommentsTest.java` and 11
hidden ones.
