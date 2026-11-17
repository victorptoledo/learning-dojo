# Sidequest: Ticket sales without overbooking

When the tickets for a big concert go on sale, thousands of people click "buy" on the same
seats in the same second. A mobile network retries a payment that already went through.
The band cancels while people are still checking out. Through all of it, a seat is sold
**once**, a payment is charged **once**, and nobody keeps a ticket for a cancelled show.

Build the ticket API for that day with Spring Boot, Spring Data JPA (H2) and Spring Security.
The starter has `TicketsApplication`, the `pom.xml`, `application.properties` and
`UsersConfig`, a few demo users kept in memory so you can focus on the sales:

| User | Password | Role |
| --- | --- | --- |
| `ada`, `linus`, `grace` | `<name>-pass` (`ada-pass`, ...) | `USER`, a buyer |
| `boss` | `boss-pass` | `ORGANIZER` |

Everything else is yours.

The contract is the HTTP API. The tests start the real server on a random port and send
**many requests at the same time** over real connections, so the locks, transactions and
constraints you choose are what keep the numbers right. They also replace your `Clock` bean
with one they move by hand, and run with other settings than the ones in
`application.properties`.

General rules:

- JSON in and out, under HTTP Basic with no session and no CSRF (a stateless API).
  Not logged in: `401`; logged in without the role: `403`.
- Errors are Problem Details (`application/problem+json`); validation errors carry an
  `errors` object that maps each bad field to a message.
- Money is a `long` number of cents; instants are ISO text in UTC (`"2099-06-01T20:00:00Z"`).
- A buyer never sees the holds or orders of another buyer: those answer `404`.

Each stage README says which endpoints and rules it adds. The tests of earlier stages keep
running, so a new rule must not break an old one.
