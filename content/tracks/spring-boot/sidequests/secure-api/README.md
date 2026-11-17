# Sidequest: Secure API

Most security bugs are not exotic: a password stored in a way that can be read back, a
form that another site can submit for you, a project that anyone who guesses its id can open,
a login that can be tried a million times. Build a small team API (users and projects) and
close each of those doors with Spring Security.

The starter has `SecureApiApplication`, the `pom.xml` (web, validation, JPA with H2 and
Spring Security) and `application.properties`. Everything else is yours: the user entity,
the security configuration, the services, the controllers and the error handling.

The contract is the HTTP API, checked with MockMvc and the Spring Security test support, plus
two beans the tests look up by type:

- a `PasswordEncoder` bean, and
- a `UserDetailsService` bean that reads the users from the database.

General rules:

- Everything lives under `/api`, in JSON. Errors are Problem Details
  (`application/problem+json`); validation errors carry an `errors` object that maps each bad
  field to a message.
- Not logged in, or wrong credentials: `401`. Logged in but not allowed: `403`. Something
  that exists but that the user may not even know about: `404`, so ids do not leak.
- The tests log in with **HTTP Basic** on most requests, so keep it on in every stage.

Each stage README says which endpoints and rules it adds. The tests of earlier stages keep
running, so a new rule must not break an old one.
