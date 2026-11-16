---
from: java
status: draft
concept: "Controllers mapped to URLs, but in the browser"
from_way: "Spring MVC maps @GetMapping(\"/products/{id}\") to a controller method that returns a view; Thymeleaf layouts wrap pages."
native_way: "React Router maps path patterns to components in the browser; layout routes wrap child routes with an Outlet; loaders fetch the data per route."
sensei_tip: "A route with a loader is a @GetMapping that runs in the browser: the loader is the controller body, useLoaderData is the model, the element is the view."
---

| Spring MVC | React Router |
| --- | --- |
| `@GetMapping("/products/{id}")` | `{ path: "products/:productId" }` |
| `@PathVariable String id` | `useParams().productId` |
| `@RequestParam String sort` | `useSearchParams().get("sort")` |
| Thymeleaf layout dialect | Layout route with `<Outlet />` |
| `return "redirect:/login"` | `throw redirect("/login")` in a loader |
| Spring Security on the server | Still needed; route guards are UX only |
