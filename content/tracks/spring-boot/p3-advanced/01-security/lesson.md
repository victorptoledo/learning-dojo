# Spring Security

Two questions protect every request: **who are you?** (authentication) and **are you allowed
to do this?** (authorization). Spring Security answers both with a chain of servlet filters in
front of your controllers. Spring Boot 4 uses **Spring Security 7**.

## What you get by adding the starter

With `spring-boot-starter-security` on the classpath and no configuration:

- every endpoint requires authentication;
- one user named `user` exists, with a random password printed at startup;
- form login and HTTP Basic are on, and the usual security headers are set.

That is a safe default, and you replace it with your own rules almost immediately.

## The filter chain

Each request passes through a `SecurityFilterChain` before reaching Spring MVC. You define it as
a bean:

```java
@Configuration
@EnableMethodSecurity
class SecurityConfig {

    @Bean
    SecurityFilterChain api(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/products/**").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/actuator/health").permitAll()
                        .anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())                        // stateless JSON API only
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .build();
    }
}
```

- Rules are checked **top to bottom**; the first match decides. Put specific rules first and
  `anyRequest()` last.
- `permitAll()`, `authenticated()`, `hasRole("ADMIN")` (checks the authority `ROLE_ADMIN`),
  `hasAuthority("orders:write")`.
- A missing or wrong login answers **401 Unauthorized**; a logged-in user without the right
  role gets **403 Forbidden**.

> [!WARNING]
> CSRF protection matters when the browser sends credentials automatically (session cookies).
> A stateless API that authenticates every request with a header (Basic or a bearer token) can
> disable it; a server-rendered app with form login must keep it.

## Users and passwords

Spring Security loads users through a `UserDetailsService`. For a small app or a kata, an
in-memory one is enough; real apps load users from a database or delegate login to an
identity provider.

```java
@Bean
UserDetailsService users(PasswordEncoder encoder) {
    return new InMemoryUserDetailsManager(
            User.withUsername("ada").password(encoder.encode("secret")).roles("USER").build(),
            User.withUsername("root").password(encoder.encode("admin")).roles("USER", "ADMIN").build());
}

@Bean
PasswordEncoder passwordEncoder() {
    return PasswordEncoderFactories.createDelegatingPasswordEncoder();   // bcrypt by default
}
```

Never store passwords in plain text or with a fast hash (MD5, SHA-256): use an adaptive hash
such as **bcrypt** or **argon2**. The delegating encoder stores the algorithm with the hash
(`{bcrypt}$2a$...`), so you can upgrade later.

## Who is the current user?

In a controller, ask for the authenticated principal:

```java
@GetMapping("/me")
Profile me(@AuthenticationPrincipal UserDetails user) {
    return new Profile(user.getUsername(), user.getAuthorities().toString());
}

@GetMapping("/notes")
List<NoteView> mine(Principal principal) {
    return notes.ownedBy(principal.getName());
}
```

Anywhere else, `SecurityContextHolder.getContext().getAuthentication()` holds it for the
current request.

## Method security

URL rules are coarse. Rules about **which object** a user may touch belong next to the code,
with method security (`@EnableMethodSecurity`):

```java
@Service
class NoteService {
    @PreAuthorize("hasRole('ADMIN')")
    List<Note> all() { ... }

    @PreAuthorize("#owner == authentication.name or hasRole('ADMIN')")
    List<Note> ownedBy(String owner) { ... }
}
```

A refused method call throws `AccessDeniedException`, answered with 403. For ownership rules
it is often clearer to check in the service and throw your own exception, or to query only the
user's data (`findByOwner(name)`) so other users' rows are never loaded.

> [!TIP]
> Answer **404**, not 403, when a user asks for an object that belongs to someone else: a 403
> tells an attacker the id exists.

## Tokens: OAuth2 resource servers

Most APIs today do not check passwords themselves. A separate **authorization server**
(Keycloak, Auth0, Entra ID, Spring Authorization Server) logs the user in and issues a signed
**JWT**; the API only validates it:

```properties
spring.security.oauth2.resourceserver.jwt.issuer-uri=https://login.example.com/realms/shop
```

```java
http.oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()));
```

Each request then carries `Authorization: Bearer <token>`; Spring verifies the signature,
the expiry and the issuer, and turns the token's scopes into authorities (`SCOPE_orders:read`).

## Testing security

With `spring-boot-starter-security-test`:

```java
mvc.perform(get("/notes").with(httpBasic("ada", "secret"))).andExpect(status().isOk());
mvc.perform(get("/notes")).andExpect(status().isUnauthorized());

@Test
@WithMockUser(username = "ada", roles = "USER")
void users_cannot_see_the_admin_page() throws Exception {
    mvc.perform(get("/admin/stats")).andExpect(status().isForbidden());
}
```

`httpBasic(...)` sends real credentials through the filter chain; `@WithMockUser` and
`.with(user("ada").roles("ADMIN"))` skip the login and act as that user. `.with(jwt())` does the
same for resource servers.

## Summary

- The starter secures everything by default; define your own `SecurityFilterChain` bean.
- Rules run top to bottom: specific first, `anyRequest()` last; 401 means not logged in, 403
  means not allowed.
- Hash passwords with bcrypt (the delegating `PasswordEncoder`).
- Read the user with `@AuthenticationPrincipal` or `Principal`; protect methods and objects with
  `@PreAuthorize` or ownership checks, and prefer 404 for other users' objects.
- APIs usually validate JWTs from an identity provider as OAuth2 resource servers.
- Test with `httpBasic(...)`, `@WithMockUser` and `jwt()`.
