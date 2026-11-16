# Errors as Problem Details (RFC 9457)

A status code says *that* something failed and roughly *who* must act. Clients also need to
know *what* failed, *which* input was wrong and *whether* they can do something about it.
Without a standard, every API invents its own error shape and every client writes a parser per
API. **RFC 9457, "Problem Details for HTTP APIs"** (2023, replacing RFC 7807) is that standard,
and Spring, ASP.NET Core, Quarkus and many gateways speak it out of the box.

## The shape

```http
HTTP/1.1 422 Unprocessable Content
Content-Type: application/problem+json
Content-Language: en

{
  "type": "https://api.shop.example/problems/insufficient-funds",
  "title": "Insufficient funds",
  "status": 422,
  "detail": "Your balance is 5.00 EUR; the payment needs 9.00 EUR.",
  "instance": "/accounts/acc-2/payments/pay_91",
  "balance": 500,
  "required": 900
}
```

| Member | Meaning | Rules of thumb |
| --- | --- | --- |
| `type` | a URI that **identifies the problem type**; the primary key for client code | stable, ideally resolvable to human docs; `about:blank` (the default) means "nothing beyond the status" |
| `title` | short human summary **of the type** | the same for every occurrence of the type; not for program logic |
| `status` | the HTTP status, repeated for convenience | must match the real status |
| `detail` | human explanation **of this occurrence** | help the user fix it; never parse it in code |
| `instance` | a URI for this occurrence | the request path, or a link to a log entry or incident |
| *extensions* | any extra members | machine-readable data for this type: `balance`, `retryAfter`, `errors` |

Clients **must** ignore extension members they do not know, so adding one is a compatible
change. Client code switches on `type`, reads extensions, and shows `detail` to humans.

## Validation errors

RFC 9457 suggests an `errors` extension for multiple problems, each pointing at the offending
part of the request with a **JSON Pointer** (RFC 6901):

```json
{
  "type": "https://api.shop.example/problems/validation-error",
  "title": "Invalid request",
  "status": 400,
  "detail": "The request has 2 invalid fields.",
  "errors": [
    {"pointer": "#/amountCents", "detail": "must be greater than 0"},
    {"pointer": "#/lines/1/sku", "detail": "unknown product"}
  ]
}
```

Report **all** field errors at once, not one per round trip, and sort them so responses are
deterministic.

## Designing problem types

- Define a **catalogue** of problem types per API (or company): id, status, title, when it
  happens, what the client should do, which extensions it carries. Publish it with the API docs.
- Make types specific enough to act on (`insufficient-funds`, `account-frozen`) but not one per
  `if` statement. A client should be able to write a `switch` over them.
- Problem types are part of the contract: renaming one breaks clients just like renaming a
  field.

## Security: what never goes into an error

Stack traces, exception class names, SQL fragments, host names, internal ids and library
versions help attackers and nobody else. A `500` says "an unexpected error occurred" plus an
`instance` or correlation id that support can find in the logs. Also beware of errors that leak
existence ("email already registered" on a public sign-up form allows account enumeration;
decide consciously).

## Implementations

| Stack | Built-in support |
| --- | --- |
| Spring Framework 6+ | `ProblemDetail`, `ErrorResponse`, `ResponseEntityExceptionHandler`; `spring.mvc.problemdetails.enabled=true` |
| ASP.NET Core 7+ | `ProblemDetails`, `AddProblemDetails()`, `Results.Problem(...)` |
| Python | FastAPI handlers returning `application/problem+json`; libraries such as `fastapi-problem` |
| Go | a small struct with `json` tags; no standard library type |
| Node.js | `http-problem-details` and framework plugins |

> The Spring Boot track's lesson "Validation and Errors" (`spring-boot.p1.validation-errors`)
> shows the Spring mechanics in detail; the next kata uses them to apply the design rules above.

## Further reading

- RFC 9457, "Problem Details for HTTP APIs": <https://www.rfc-editor.org/rfc/rfc9457>
- RFC 6901, "JavaScript Object Notation (JSON) Pointer": <https://www.rfc-editor.org/rfc/rfc6901>
- IANA HTTP Problem Types registry: <https://www.iana.org/assignments/http-problem-types/http-problem-types.xhtml>
- Spring Framework reference, "Error Responses": <https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-rest-exceptions.html>
- Zalando RESTful API Guidelines, "Use Problem JSON": <https://opensource.zalando.com/restful-api-guidelines/#176>
- OWASP, "Error Handling Cheat Sheet": <https://cheatsheetseries.owasp.org/cheatsheets/Error_Handling_Cheat_Sheet.html>
