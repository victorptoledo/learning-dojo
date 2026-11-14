# Todo API

Build the `/todos` resource. The records `Todo`, `NewTodo` and `TodoUpdate` are written; you
write the `TodoService` (in memory) and the `TodoController`.

## The service

`TodoService` is a singleton `@Service`, so it is shared by every request: store the todos in a
thread-safe map ordered by id (a `ConcurrentSkipListMap<Long, Todo>`), and take ids from an
`AtomicLong` that starts at 1 and **never reuses** an id. Titles are stored **stripped**.

## The endpoints

| Request | Success | Failure |
| --- | --- | --- |
| `POST /todos` `{"title": "..."}` | `201 Created`, `Location: .../todos/{id}`, the new todo (`done` false) | `400` when the title is missing or blank |
| `GET /todos` | `200`, every todo by id | |
| `GET /todos?done=true` (or `false`) | `200`, only the done (or open) ones | |
| `GET /todos/{id}` | `200`, the todo | `404` when it does not exist |
| `PUT /todos/{id}` `{"title": "...", "done": true}` | `200`, the updated todo | `400` blank title, `404` unknown id |
| `DELETE /todos/{id}` | `204 No Content` | `404` unknown id |

```json
POST /todos  {"title": "  learn spring "}
→ 201  Location: http://localhost/todos/1
       {"id": 1, "title": "learn spring", "done": false}
```

> Tip: `@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping`, `@PathVariable`,
> `@RequestParam(required = false) Boolean done`, `@RequestBody`, `ResponseEntity.of(optional)`
> (200 or 404), `ResponseEntity.created(uri)`, `ResponseEntity.noContent()`, and a
> `UriComponentsBuilder` parameter to build the location.
