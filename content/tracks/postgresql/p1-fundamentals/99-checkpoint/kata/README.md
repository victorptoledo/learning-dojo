# Checkpoint 1: Room Bookings

A meeting-room service is moving to PostgreSQL rules. `fixture/01_rooms.sql` creates what the
old system left:

```text
rooms    (id, name, capacity, amenities text[])
bookings (id, room_id → rooms, organiser text, status text, during tstzrange,
          attendees text[], created_at)
```

A booking is **confirmed**, **requested** or **cancelled**. Only confirmed bookings take a room.
All times are UTC.

| File | Kind | Task |
| --- | --- | --- |
| `constraints.sql` | statements | Make the bookings table safe, in this order. (1) The old system let double bookings through: every confirmed booking that overlaps an **earlier-created** confirmed booking of the same room becomes `cancelled` (break `created_at` ties by `id`). (2) A domain `email` (text with one `@`, something before and after it, no spaces) for `organiser`, and `attendees` becomes an array of it. (3) `status` must be one of the three values. (4) `during` must be a half-open period `[start, end)` with both a start and an end, and not empty. (5) **No two confirmed bookings of the same room may overlap**, enforced by the database. |
| `free_rooms.sql` | one query | Rooms for at least 8 people that have a `projector` and no confirmed booking overlapping 2026-11-16 14:00–15:00: `id`, `name`, by capacity then name. |
| `busy_hours.sql` | one query | When room 1 is busy on 2026-11-16, as periods: overlapping or touching confirmed bookings are **merged** (09:00–10:30 and 10:30–11:00 are one period 09:00–11:00). `starts_at`, `ends_at`, in time order. |
| `settings.sql` | statements | Create a login role `booking_api` whose sessions get a 5 second `statement_timeout` and a 1 minute `idle_in_transaction_session_timeout`, and make autovacuum vacuum `bookings` after 2% of its rows changed and analyze it after 1%. |

The constraint in step (5) needs the extension `btree_gist`, which ships with PostgreSQL:
`CREATE EXTENSION IF NOT EXISTS btree_gist;`. The two queries read the fixture's data and do not
depend on your `constraints.sql`.

Try it with `docker run -it --rm --network none -v "${PWD}:/work" dojo-sql play`. Run
`\i constraints.sql` inside `BEGIN;` ... `ROLLBACK;` to try it as many times as you like.

There are 18 tests: 1 in `tests/visible.sql` and 17 hidden ones. Several hidden tests insert
bookings after your `constraints.sql` and expect them to be accepted or rejected with the right
SQLSTATE (`23P01` for an exclusion, `23514` for a check).
