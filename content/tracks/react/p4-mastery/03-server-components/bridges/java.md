---
from: java
status: draft
concept: "Server-side views and client widgets in one tree"
from_way: "Spring MVC with Thymeleaf renders HTML on the server and sprinkles JavaScript widgets on top; REST controllers expose endpoints for the client."
native_way: "Server Components render on the server like Thymeleaf views, Client Components are the widgets, and server functions are endpoints the framework generates for you."
sensei_tip: "A server function is a @PostMapping you did not have to write, so give it the same treatment: @PreAuthorize-style checks and Bean Validation-style input validation inside."
---

| Java / Spring | React frameworks |
| --- | --- |
| Thymeleaf view rendered per request | Server Component (SSR) |
| JavaScript widget on the page | Client Component (`"use client"`) |
| `@PostMapping` + `@Valid` | Server function with zod validation |
| Spring Security on the endpoint | `auth()` check inside the server function |
| Model attributes | Serialisable props across the boundary |
| Static site generator | SSG |
