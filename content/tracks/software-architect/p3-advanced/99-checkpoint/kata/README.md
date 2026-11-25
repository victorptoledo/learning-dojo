# Trip Booking Saga

`dojo.travel` books a trip as a flight, a hotel and a car, each with its own supplier. The
suppliers answer through a message channel that delivers **at least once**, so the same reply
can arrive twice. Today:

- `Trip` is anemic, with setters for everything, and applies `SupplierReply` messages itself,
  so the domain depends on the messaging package;
- `TripBookingSaga` uses `InMemoryTrips` directly, asks for the next leg after any reply, and
  when a supplier refuses, it cancels the trip and **leaves the legs already reserved booked**;
- a duplicate reply asks for the next leg again, and a reply nobody expects crashes the saga.

`Leg`, `TripStatus` and everything in `dojo.travel.messaging` (`SupplierCommand`,
`SupplierReply`, `CommandBus`, `DeadLetterChannel`) are given: do not change them.

## The target design

| Type | What it is |
| --- | --- |
| `domain.TripId` | **record** `(String value)`, not blank, or `IllegalArgumentException` |
| `domain.Trip` | the aggregate root: a **final** class with no public constructor and no setters |
| `domain.TripEvent` | a **sealed** interface with `TripId tripId()`; records `TripConfirmed(TripId tripId)` and `TripCancelled(TripId tripId, String reason)` implement it |
| `domain.Trips` | the repository port: `void save(Trip)`, `Optional<Trip> find(TripId)` |
| `adapters.InMemoryTrips` | implements `Trips` |
| `application.TripBookingSaga` | `TripBookingSaga(Trips trips, CommandBus bus, Consumer<TripEvent> events, DeadLetterChannel deadLetters)` |
| `dojo.travel.App` | `static TripBookingSaga create(CommandBus bus, Consumer<TripEvent> events, DeadLetterChannel deadLetters)`: the composition root |

### The trip

- `static Trip request(TripId id, String traveller, List<Leg> legs)`: a traveller that is not
  blank and at least one leg, each leg at most once (`IllegalArgumentException`). The legs are
  booked in the order given. The trip starts `PENDING`.
- `id()`, `traveller()`, `status()`; `legs()` cannot be changed from outside;
  `Optional<String> confirmation(Leg)`; `Optional<Leg> nextLeg()` is the first leg with no
  confirmation while the trip is pending, and empty otherwise.
- `reserved(Leg leg, String confirmation)`: only while pending (`IllegalStateException("trip is
  " + status)`), only for the next leg (`IllegalStateException("expected FLIGHT, got HOTEL")`)
  and with a confirmation that is not blank (`IllegalArgumentException`). The last leg
  confirms the trip and records `TripConfirmed`.
- `List<Leg> cancel(String reason)`: only while pending, with a reason that is not blank. The
  trip becomes `CANCELLED`, records `TripCancelled`, and returns the legs to release: the
  reserved ones, **latest first**.
- `List<TripEvent> pullEvents()` returns the events recorded since the last call and forgets
  them. A refused command changes nothing.

### The saga

- `TripId start(String tripId, String traveller, List<Leg> legs)`: an id that exists throws
  `IllegalArgumentException("trip exists: " + tripId)`. It saves the trip and sends
  `SupplierCommand.reserve` for the first leg.
- `void handle(SupplierReply reply)`, in this order:
  1. a message id it has already handled is **ignored**: no command, no event, no dead letter;
  2. an unknown trip goes to the dead letter channel with the reason `"unknown trip"`;
  3. a **pending** trip: a reply for any leg but the next one is dead-lettered with
     `"unexpected reply for " + leg` and changes nothing. A reservation is recorded, then the
     saga asks for the next leg, or publishes `TripConfirmed` after the last one. A refusal
     cancels the trip with the reason `leg + " refused: " + detail`
     (`"HOTEL refused: fully booked"`), sends `SupplierCommand.release` for each leg the trip
     returns, in that order, and publishes `TripCancelled`;
  4. a **cancelled** trip: a reservation for a leg the trip has no confirmation for arrived too
     late and would stay booked forever, so the saga releases it. Anything else is ignored;
  5. a **confirmed** trip: the reply is dead-lettered with `"trip is confirmed"`.
- `void cancel(String tripId, String reason)`: the traveller cancels. An unknown trip throws
  `IllegalArgumentException("unknown trip " + tripId)`; a trip that is not pending throws
  `IllegalStateException`. Otherwise, like a refusal: release, then publish `TripCancelled`.

Releases are fire and forget: the suppliers confirm them out of band, and this kata does not
track them.

## What the grading tests check

- The graph, read from the compiled classes: the domain uses nothing else of `dojo.travel`; the
  messages know only the domain; only `App` knows the adapters; no package cycle.
- `Trip` is final, with no public constructor, no setters and only private fields; `TripId` and
  the events are records; `TripEvent` is sealed; `Trips` is an interface.
- Every rule of the trip, including that refused commands change nothing.
- The saga: the order of the commands, compensations, duplicates, dead letters, a cancellation
  by the traveller, a reservation that arrives after it, and several trips at once.

The visible tests describe the target design, so they do not compile until it exists. Keep your
tests in the package `dojo`.
