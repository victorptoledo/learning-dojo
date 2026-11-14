# Validation and Problem Details

Every API gets bad input: missing fields, wrong formats, impossible values, requests that
conflict with the current state. A good API rejects them **early**, with the **right status**
and a body that tells the client exactly what to fix. Spring gives you two standard tools:
**Jakarta Bean Validation** for the input, and **Problem Details** (RFC 9457) for the errors.

## Bean Validation on records

Annotate the components of the input record with constraints:

```java
public record SignupRequest(
        @NotBlank @Size(max = 40) String name,
        @NotBlank @Email String email,
        @NotNull @Min(13) Integer age,
        @Pattern(regexp = "[a-z]{2}") String country) {
}
```

Common constraints (package `jakarta.validation.constraints`):

| Constraint | Checks |
| --- | --- |
| `@NotNull` | not null |
| `@NotBlank` | a String that is not null and not only whitespace |
| `@NotEmpty` | a String, collection or map that is not null and not empty |
| `@Size(min, max)` | length of a String, size of a collection |
| `@Min`, `@Max`, `@Positive`, `@PositiveOrZero` | numbers |
| `@Email`, `@Pattern(regexp)` | formats |
| `@Past`, `@Future` | dates |
| `@Valid` on a component | validate the nested record too |

Constraints other than `@NotNull`/`@NotBlank`/`@NotEmpty` **accept `null`**: `@Email` on a
`null` value passes. Combine them (`@NotBlank @Email`) when the field is required.

Then ask Spring MVC to validate the body with `@Valid`:

```java
@PostMapping("/signups")
ResponseEntity<SignupView> signup(@Valid @RequestBody SignupRequest request, UriComponentsBuilder uri) {
    ...
}
```

When a constraint fails, your method **never runs**: Spring throws
`MethodArgumentNotValidException` and answers `400 Bad Request`. For constraints on
`@PathVariable` or `@RequestParam` parameters (`@Min(1) @PathVariable long id`), Spring 7
validates them too and answers `400` through `HandlerMethodValidationException`.

## Problem Details: one shape for every error

RFC 9457 defines a JSON body for errors, with the media type `application/problem+json`:

```json
{
  "type": "about:blank",
  "title": "Conflict",
  "status": 409,
  "detail": "email ada@example.com is already registered",
  "instance": "/signups"
}
```

You may add your own fields (here `errors`) for details clients need. Spring's class for it is
`ProblemDetail`. Without any code, one property makes Spring answer its own errors (400 on bad
JSON, 404 on unknown paths, 405, 415, ...) this way:

```properties
spring.mvc.problemdetails.enabled=true
```

## Mapping your exceptions

Domain code should throw exceptions that mean something in the domain, not HTTP:

```java
public class EmailTakenException extends RuntimeException {
    public EmailTakenException(String email) {
        super("email " + email + " is already registered");
    }
}
```

and **one** place translates them into responses, a `@RestControllerAdvice`. Extending
`ResponseEntityExceptionHandler` gives you Problem Details for all of Spring's own errors
(malformed JSON, unknown paths, wrong method, validation) and lets you override the ones you
want to shape:

```java
@RestControllerAdvice
class ApiErrors extends ResponseEntityExceptionHandler {

    @ExceptionHandler(EmailTakenException.class)
    ProblemDetail emailTaken(EmailTakenException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
        problem.setTitle("Email already registered");
        return problem;
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException e, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "invalid request");
        Map<String, String> errors = new TreeMap<>();
        for (FieldError error : e.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        problem.setProperty("errors", errors);
        return handleExceptionInternal(e, problem, headers, status, request);
    }
}
```

- An `@ExceptionHandler` method can return `ProblemDetail` directly; Spring sets the status
  from it and writes `application/problem+json`.
- The handler for the most specific exception type wins.
- For exceptions `ResponseEntityExceptionHandler` already handles, **override** its method
  (`handleMethodArgumentNotValid`, `handleHttpMessageNotReadable`, ...) instead of adding an
  `@ExceptionHandler` for the same type: two handlers for one exception is an error.
- With your own `ResponseEntityExceptionHandler` bean, Boot's built-in one (switched on by
  `spring.mvc.problemdetails.enabled`) backs off.
- `ErrorResponseException` (and its subclasses) are exceptions that already carry a
  `ProblemDetail`; throw `new ErrorResponseException(HttpStatus.NOT_FOUND)` from anywhere.

> [!WARNING]
> Never put stack traces, SQL or internal class names into an error response. Log the details
> on the server; give the client a stable, readable `detail`.

## Which status for which problem

| Problem | Status |
| --- | --- |
| Malformed JSON, a missing or invalid field | `400 Bad Request` |
| Not authenticated / not allowed | `401` / `403` |
| The resource does not exist | `404 Not Found` |
| The request conflicts with the current state (duplicate, version mismatch) | `409 Conflict` |
| Well-formed but semantically impossible (a business rule) | `422 Unprocessable Content` (or 400) |
| A bug or an outage on the server | `500` / `503`; never for client mistakes |

## Testing errors

```java
mvc.perform(post("/signups").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name": "", "email": "nope", "age": 10}
                        """))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.errors.email").exists())
        .andExpect(jsonPath("$.errors.age").exists());
```

## Summary

- Put Bean Validation constraints on input records and `@Valid` on `@RequestBody`; invalid
  input becomes `400` before your code runs.
- Answer errors as Problem Details (`application/problem+json`); enable them for Spring's own
  errors with `spring.mvc.problemdetails.enabled=true`.
- Throw domain exceptions; translate them in one `@RestControllerAdvice`.
- Choose the status by the kind of problem, and never leak internals.
