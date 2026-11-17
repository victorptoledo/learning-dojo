# Sidequest: Seat booking

A theatre sells its tickets online and at two box offices, all at once. Every bug in this
system is a race: two people get the same seat, a show sells more standing tickets than the
room holds, a customer gets around a limit by clicking twice. You build the box office in
the database, as tables, constraints and PL/pgSQL functions, in five stages:

1. booking a numbered seat, and cancelling;
2. standing tickets limited by the show's capacity;
3. at most four tickets per customer and show;
4. rehearsal rooms booked by the hour, never twice at the same time;
5. a waiting list that two box offices work through together.

```text
shows     (id, title, starts_at, standing_capacity)
seats     (show_id → shows, seat)        -- 'A1' ... 'B5' for every show
customers (id, name)
rooms     (id, name)
```

How it is checked:

- `fixture/` holds the tables above; you write one file per stage (`booking.sql`,
  `standing.sql`, `rooms.sql`, `waitlist.sql`) with your tables and functions. Each stage's
  statement gives the names and signatures the tests call; how you write them is yours.
- The dojo loads each file once and **commits** it, then runs two kinds of tests:
  - ordinary tests call your functions inside a transaction that is rolled back;
  - **race tests** open two or three real sessions (with `dblink`), start a transaction in
    one, call your function in another while the first is still open, and check who waited,
    who won and what the tables hold after both finish.
- So the files must load in one go on an empty database: no `BEGIN`/`COMMIT`, no `psql`
  commands. Later stages may change the functions of earlier files.
- Try things with `docker run -it --rm --network none -v "${PWD}:/work" dojo-sql play` from
  this folder. Open a second terminal with `docker exec -it <container> psql -d dojo` to
  play the second session yourself: `BEGIN; SELECT book_seat(...);` in one, the same in the
  other, and watch it wait.

When you are done, `[A]` copies a review prompt: ask an AI where your locks could still let
a race through.
