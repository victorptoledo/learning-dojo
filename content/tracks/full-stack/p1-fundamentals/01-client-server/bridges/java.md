---
from: java
status: reviewed
concept: "CORS and cookies from Spring's side"
from_way: "@CrossOrigin(\"*\") on a controller until the console error goes away, and session cookies left at their defaults."
native_way: "One CorsConfigurationSource with the exact origins, credentials only where needed, and session cookies set Secure, HttpOnly and SameSite."
sensei_tip: "With Spring Security, configure CORS in the security filter chain (http.cors), or preflights are rejected before your @CrossOrigin is ever read."
---

| Spring | Browser effect |
| --- | --- |
| `server.servlet.session.cookie.same-site=lax` | the session is not sent on cross-site POSTs |
| `CorsConfiguration.setAllowCredentials(true)` | needs an exact origin, not `*` |
| `ResponseEntity.status(303).location(...)` | Post/Redirect/Get |
| `@GetMapping` that deletes | prefetchers and scanners trigger it |
