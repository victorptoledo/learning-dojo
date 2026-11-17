# Sidequest: Wallet with transfers

Money is the classic place where "it works on my machine" is not enough. Two requests
arrive in the same millisecond, a transfer fails half way, the server restarts: the balances
must still be right, to the cent.

Build a wallet API on **Spring Data JPA** with the H2 database Spring Boot starts for you.
The starter has only `WalletApplication`, the `pom.xml` and `application.properties`; the
entities, repositories, services, controllers and error handling are yours.

The contract is the HTTP API, checked with MockMvc:

- Money is always a `long` number of **cents**, in fields that end in `Cents`.
- JSON in and out; a created resource answers `201` with a `Location` header.
- Every error is a Problem Detail (`application/problem+json`) with the right `status`;
  validation errors carry an `errors` object that maps each bad field to a message.
- The data lives **in the database**, not in a map: one test restarts the application on a
  file database and expects the accounts to be there.
- Some tests count the SQL statements a request runs (the starter turns Hibernate statistics
  on for that), so think about how many queries each endpoint needs.

Each stage README says which endpoints and rules it adds. The tests of earlier stages keep
running, so a new rule must not break an old one.
