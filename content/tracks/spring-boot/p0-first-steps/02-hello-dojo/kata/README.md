# Hello, Dojo (Spring Boot)

Write the endpoint in `src/main/java/dojo/GreetingController.java`:

```text
GET /greet?name=Ada        ->  200  "Welcome to the dojo, Ada!"
GET /greet?name=%20Ada%20  ->  200  "Welcome to the dojo, Ada!"       (spaces around the name are ignored)
GET /greet                 ->  200  "Welcome to the dojo, stranger!"  (no name)
GET /greet?name=           ->  200  "Welcome to the dojo, stranger!"  (blank name)
```

The response is plain text (a `String` returned from a `@RestController` method).

What you start with:

```java
@RestController
public class GreetingController {
    @GetMapping("/greet")
    public String greet(@RequestParam(required = false) String name) {
        throw new UnsupportedOperationException("todo greet " + name);
    }
}
```

- `@GetMapping("/greet")` already maps the request to the method.
- `@RequestParam(required = false)` gives you `?name=...`, or `null` when it is missing.
- Then it is the same logic as the Java hello kata: `null` or blank means stranger,
  otherwise `strip()` the name.

Run the app with `mvn spring-boot:run` and open `http://localhost:8080/greet?name=Ada` if you
want to see it in a browser. The tests do not need a running server.
