Reject bad input at the door with Pydantic, and complete the CRUD.

- `title` has spaces around it removed and must then hold 1 to 200 characters. `done` must be
  a real JSON boolean (`"yes"` or `1` are refused). A body with other fields, or without a
  title on `POST`, is refused. Every refusal is FastAPI's `422` with its `detail` list.
- `PATCH /tasks/{id}` changes only the fields it is given (`title`, `done`, or both, validated
  the same way) and returns `200` with the whole task; an empty body changes nothing.
- `DELETE /tasks/{id}` returns `204` with no body.
- `PATCH` and `DELETE` of a task that does not exist return `404` with
  `{"detail": "task 7 not found"}`, like `GET`.
