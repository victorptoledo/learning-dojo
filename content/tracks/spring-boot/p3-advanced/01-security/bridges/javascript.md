---
from: javascript
status: draft
concept: "Securing endpoints with Spring Security"
from_way: "Passport strategies, guards in NestJS, and middleware checking req.user."
native_way: "Filters in a SecurityFilterChain and authorisation rules."
sensei_tip: "A NestJS guard is an authorisation rule or @PreAuthorize; Passport strategies are Spring Security's authentication providers and filters."
---

| JavaScript / TypeScript | Spring Boot 4 |
| --- | --- |
| passport.authenticate('jwt') | oauth2ResourceServer().jwt() |
| @UseGuards(RolesGuard) | @PreAuthorize("hasRole('ADMIN')") |
| bcrypt.hash | PasswordEncoder.encode |
| req.user | Authentication principal |
