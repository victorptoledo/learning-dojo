---
from: java
status: draft
concept: "From servlets and JAX-RS to Spring MVC"
from_way: "HttpServlet with doGet and doPost parsing parameters by hand, or JAX-RS resources, returning entities and 200 for everything."
native_way: "@RestController methods with @GetMapping and friends, path variables and JSON records bound for you, and ResponseEntity for status and headers."
sensei_tip: "Write the MockMvc test for the status code first: it forces you to decide what each failure answers before writing the happy path."
---

| Servlet / JAX-RS | Spring MVC |
| --- | --- |
| `doGet(req, resp)` + `req.getParameter("id")` | `@GetMapping("/{id}")` + `@PathVariable long id` |
| `@Path("/todos") @GET` | `@RequestMapping("/todos")` + `@GetMapping` |
| `Response.status(201).entity(x).build()` | `ResponseEntity.created(uri).body(x)` |
| Hand-written JSON with Gson or Jackson 2 | Records serialised by Jackson 3 automatically |
