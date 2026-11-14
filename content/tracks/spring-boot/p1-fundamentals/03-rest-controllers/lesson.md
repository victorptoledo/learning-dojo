# REST Controllers with Spring MVC

A REST API maps **resources** to URLs and uses HTTP's own vocabulary to act on them: methods
say *what* to do, status codes say *what happened*. Spring MVC lets you write that as plain
methods on a controller.

## The HTTP vocabulary you need

| Method | Meaning | Typical success |
| --- | --- | --- |
| `GET /todos` | list | `200 OK` |
| `GET /todos/7` | read one | `200 OK`, or `404 Not Found` |
| `POST /todos` | create | `201 Created` with a `Location` header |
| `PUT /todos/7` | replace | `200 OK` (or `204`) |
| `PATCH /todos/7` | change some fields | `200 OK` |
| `DELETE /todos/7` | delete | `204 No Content` |

Client errors are `4xx`: `400` bad input, `401` not logged in, `403` not allowed, `404` not
found, `409` conflict with the current state, `422` valid syntax but impossible request.
Server errors are `5xx`.

## A controller

```java
@RestController
@RequestMapping("/todos")
public class TodoController {
    private final TodoService todos;

    public TodoController(TodoService todos) {
        this.todos = todos;
    }

    @GetMapping
    public List<TodoView> list(@RequestParam(defaultValue = "false") boolean onlyOpen) {
        return todos.list(onlyOpen);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TodoView> one(@PathVariable long id) {
        return todos.find(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<TodoView> create(@RequestBody NewTodo body, UriComponentsBuilder uri) {
        TodoView created = todos.create(body.title());
        URI location = uri.path("/todos/{id}").build(created.id());
        return ResponseEntity.created(location).body(created);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) {
        todos.delete(id);
    }
}
```

### Mapping

- `@RequestMapping("/todos")` on the class sets a prefix; `@GetMapping`, `@PostMapping`,
  `@PutMapping`, `@PatchMapping` and `@DeleteMapping` on methods add the rest.
- `{id}` in the path is a **path variable**, read with `@PathVariable`. The name must match
  the parameter (the compiler keeps parameter names with `-parameters`, which the Boot parent
  enables), or give it explicitly: `@PathVariable("id") long todoId`.
- `@RequestParam` reads the query string (`?onlyOpen=true`). Set `required = false` or a
  `defaultValue` when it is optional.
- `@RequestHeader("X-Request-Id")` reads a header.

Spring converts the text to the parameter type (`long`, `boolean`, `LocalDate`, enums). A
value that does not convert is answered with `400` before your method runs.

### Bodies: records in, records out

```java
public record NewTodo(String title) {}
public record TodoView(long id, String title, boolean done) {}
```

`@RequestBody` turns the JSON body into the record, and the return value is written back as
JSON. Spring Boot 4 uses **Jackson 3** (package `tools.jackson`), which reads and writes
records without any annotation. JSON field names follow the component names.

> [!IMPORTANT]
> Use separate records for what comes **in** (`NewTodo`: only what the client may send) and
> what goes **out** (`TodoView`: what the client may see). Exposing your domain or database
> objects directly lets clients set fields they should not (an `id`, a `role`) and ties your
> API to your storage.

### Responses: status and headers

- Return the body directly for a plain `200`.
- Return `ResponseEntity<T>` to choose status and headers: `ResponseEntity.ok(body)`,
  `created(location).body(...)`, `noContent().build()`, `notFound().build()`,
  `status(HttpStatus.CONFLICT).body(...)`.
- `@ResponseStatus(HttpStatus.NO_CONTENT)` fixes the status of a `void` method.
- `UriComponentsBuilder` (injected as a parameter) builds the `Location` URL from the current
  request.

## Keep controllers thin

A controller **translates** between HTTP and your application: it reads the request, calls a
service, and turns the result into a response. Business rules (what makes a todo valid, what
happens on completion) belong in the service, which you can test without HTTP.

```text
HTTP request → Controller (parse, validate shape) → Service (rules) → Repository (storage)
```

## Testing a controller with MockMvc

```java
mvc.perform(post("/todos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title": "learn spring"}
                        """))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "http://localhost/todos/1"))
        .andExpect(jsonPath("$.title").value("learn spring"))
        .andExpect(jsonPath("$.done").value(false));

mvc.perform(get("/todos")).andExpect(jsonPath("$.length()").value(1));
```

`jsonPath` reads the response with JsonPath expressions: `$.title`, `$[0].id`,
`$.length()`. MockMvc's base URL is `http://localhost`, which is why the `Location` header
starts with it.

## Summary

- Map resources to URLs; use the HTTP method for the action and the status code for the
  result (`201` + `Location` on create, `204` on delete, `404` when missing).
- `@PathVariable`, `@RequestParam` and `@RequestBody` bind the request; records carry JSON
  in and out, with different records for input and output.
- `ResponseEntity` chooses status and headers.
- Controllers stay thin and delegate to services; MockMvc tests them with `jsonPath`.
