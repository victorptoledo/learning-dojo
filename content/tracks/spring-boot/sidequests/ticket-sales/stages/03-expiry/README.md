A buyer who holds seats and walks away must not keep them: a hold lasts `tickets.hold-ttl`.

From the instant `expiresAt` on (by the application `Clock`, `expiresAt` itself included):

- the hold answers with `status` `EXPIRED`, and releasing it is `409` (`hold <id> is not
  active`);
- its seats are `AVAILABLE` again in `GET /events/{id}/seats`, count in `available`, and any
  buyer may hold them;
- if someone else takes some of those seats, the expired hold stays `EXPIRED`: it never comes
  back to life, not even for the seats nobody took.

This has to be true **at once**, on the very next request after the clock passes `expiresAt`,
not when a background job happens to run. Decide "is this seat free?" with the time in mind:
a seat held until an instant that has passed is free. (A `@Scheduled` job that tidies up old
holds in the database is a fine addition, but the tests do not wait for one.)

The races of the last stage still apply: two dozen buyers going for a seat whose hold has
just expired still produce exactly one winner.

> Tip: keep on the seat the instant until which it is held, and count a seat as free when it
> is `AVAILABLE` or held until an instant that is not after now. The status of a hold can be
> worked out when you read it, from its stored status and `expiresAt`.
