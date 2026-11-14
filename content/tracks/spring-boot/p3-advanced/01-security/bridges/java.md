---
from: java
status: draft
concept: "From WebSecurityConfigurerAdapter to SecurityFilterChain beans"
from_way: "Extending WebSecurityConfigurerAdapter and overriding configure(HttpSecurity), antMatchers, @EnableGlobalMethodSecurity, and passwords hashed with MD5 or stored as is."
native_way: "A SecurityFilterChain @Bean with the lambda DSL, requestMatchers, @EnableMethodSecurity with @PreAuthorize, the delegating bcrypt encoder, and JWT resource servers."
sensei_tip: "Write the security tests (401, 403, 404 for someone else's object) before the rules: they are the specification."
---

| Spring Security 5 | Spring Security 7 (Boot 4) |
| --- | --- |
| `extends WebSecurityConfigurerAdapter` | `@Bean SecurityFilterChain` |
| `.antMatchers("/admin/**")` | `.requestMatchers("/admin/**")` |
| `.and()` chains | lambda DSL: `.authorizeHttpRequests(auth -> ...)` |
| `@EnableGlobalMethodSecurity(prePostEnabled = true)` | `@EnableMethodSecurity` |
| `NoOpPasswordEncoder` / MD5 | `PasswordEncoderFactories.createDelegatingPasswordEncoder()` |
