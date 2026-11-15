---
from: go
status: draft
concept: "Securing endpoints with Spring Security"
from_way: "Middleware that checks a session or token, written or chosen per project."
native_way: "A configured filter chain with rules, encoders and method security."
sensei_tip: "Spring Security is a chain of middleware like the ones you write in Go, already implemented. Learn the order of the rules: the first match wins."
---

| Go | Spring Boot 4 |
| --- | --- |
| auth middleware | SecurityFilterChain |
| bcrypt.GenerateFromPassword | BCryptPasswordEncoder |
| ctx value with the user | SecurityContextHolder |
| per-route checks | requestMatchers(...).hasRole(...) |
