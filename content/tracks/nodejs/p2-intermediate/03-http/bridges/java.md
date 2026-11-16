---
from: java
status: draft
concept: "HTTP servers and clients"
from_way: "A servlet container or Spring MVC maps requests to controller methods, parses bodies into objects with Jackson, and HttpClient calls other services with a timeout."
native_way: "node:http gives raw request and response streams; routing, body parsing, validation and error handling are yours or a framework's; fetch is the client."
sensei_tip: "node:http sits at the level of the Servlet API (HttpServletRequest and Response), not Spring MVC. Express and Fastify are the Spring MVC layer: routing, binding, error handlers."
---

| Java | Node.js |
| --- | --- |
| `HttpServlet.doGet(req, resp)` | `createServer((req, res) => ...)` |
| `@GetMapping("/users/{id}")` | Express or Fastify `get("/users/:id")` |
| `@RequestBody` + Jackson | Read the stream, `JSON.parse`, validate |
| `@ExceptionHandler` | One `.catch` around the router |
| `HttpClient` with `.timeout()` | `fetch` with `AbortSignal.timeout()` |
| `server.tomcat.max-http-form-post-size` | Your own body limit, or the framework's |
