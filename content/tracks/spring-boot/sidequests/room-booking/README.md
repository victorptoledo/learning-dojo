# Sidequest: Room booking API

An office has a handful of meeting rooms and a lot of people who want them at the same
time. Build the small REST API behind its booking screen: rooms, bookings that never
overlap, and the rules of the building (opening hours, the longest meeting) read from
configuration instead of being written in the code.

The starter is a Spring Boot project with nothing in it but `RoomBookingApplication`, the
`pom.xml` and `application.properties`. Every class is yours: the records the API reads and
writes, the services that keep the data, the controllers and the error handling. Keep the
data **in memory** (a map in a service is enough; JPA comes in the next level), and make the
services safe when two requests arrive at once.

The contract is the HTTP API, checked with MockMvc:

- JSON in and out; a created resource answers `201` with a `Location` header.
- **Every error is a Problem Detail** (`application/problem+json`) with the right `status`.
  Validation errors carry an `errors` object that maps each bad field to a message:
  `{"status": 400, "errors": {"capacity": "must be between 1 and 100"}}`.
- Date-times are local to the building, as ISO text without a zone: `"2099-03-02T09:00"`
  (Jackson may add the seconds when it writes them, `"2099-03-02T09:00:00"`, and that is fine).

Each stage README says which endpoints and rules it adds. The tests of earlier stages keep
running, so a new rule must not break an old one.
