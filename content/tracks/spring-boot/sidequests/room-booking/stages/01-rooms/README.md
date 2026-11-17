Rooms first. A room has an `id` (given by the API, starting at 1), a `name` and a
`capacity` (how many people fit).

| Request | Answer |
| --- | --- |
| `POST /rooms` `{"name": "Ada", "capacity": 8}` | `201`, `Location: .../rooms/{id}`, the room `{"id", "name", "capacity"}` |
| `GET /rooms` | `200`, every room, by id |
| `GET /rooms/{id}` | `200`, the room |
| `DELETE /rooms/{id}` | `204` |

Rules:

- The name is stored **stripped** of spaces around it. It must not be blank and has at most
  40 characters; the capacity goes from 1 to 100 and is required. A bad body is `400` with an
  `errors` object naming each bad field (`name`, `capacity`).
- Names are unique **ignoring case**: a second `ada` is `409`.
- An unknown room is `404` with the detail `room <id> not found`.
- A body that is not JSON is `400` too, also as a Problem Detail.

> Tip: a `record` with `@NotBlank`, `@Size` and `@Min`/`@Max` (use `Integer` so a missing
> value fails `@NotNull`), `@Valid @RequestBody`, `ResponseEntity.created(uri)`, and a
> `@RestControllerAdvice` that extends `ResponseEntityExceptionHandler`: it already turns
> Spring's own errors into Problem Details, and you override `handleMethodArgumentNotValid`
> to add the `errors` object.
