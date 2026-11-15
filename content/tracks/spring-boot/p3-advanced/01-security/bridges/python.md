---
from: python
status: draft
concept: "Securing endpoints with Spring Security"
from_way: "Django's auth and permissions, or FastAPI security dependencies with OAuth2PasswordBearer."
native_way: "A SecurityFilterChain bean with request rules, a PasswordEncoder, and method or ownership checks."
sensei_tip: "Spring Security protects everything by default as soon as the starter is on the classpath, the opposite of FastAPI where each route opts in with a dependency."
---

| Python | Spring Boot 4 |
| --- | --- |
| Depends(get_current_user) | authentication from the SecurityContext |
| @permission_classes | authorizeHttpRequests rules or @PreAuthorize |
| passlib bcrypt | BCryptPasswordEncoder |
| request.user | @AuthenticationPrincipal |
