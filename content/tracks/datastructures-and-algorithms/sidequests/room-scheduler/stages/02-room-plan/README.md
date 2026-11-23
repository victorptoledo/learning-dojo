Tomorrow's meetings are known; the rooms are not assigned yet. Two classic interval problems,
both solved by sorting first. The starter adds the records `Request(String id, long start, long
end)` and `Plan(int rooms, Map<String, Integer> roomOf)`, and the class `Planner` with two static
methods to fill in.

## `static Plan plan(List<Request> requests)`

Assign every request a room numbered from `0`, using **as few rooms as possible**, by this rule:
take the requests by start (ties by id), and give each one the **lowest-numbered room that is
free** at its start (a room is free when its last meeting ended at or before the start); open a
new room only when none is free. `rooms` is how many rooms were opened, and `roomOf` maps each id
to its room.

```text
a [0, 30)   b [10, 40)   c [30, 60)   d [35, 50)
a → 0, b → 1, c → 0 (a ended at 30), d → 2 (0 and 1 are busy): rooms = 3
```

This greedy rule is optimal: a new room opens only when every room is busy, so at that moment
that many meetings overlap. Two heaps make it O(n log n): the busy rooms ordered by the end of
their meeting, and the free rooms ordered by number. Scanning every room for each request is
O(n · rooms), and the tests have days where 50,000 meetings overlap.

## `static List<Request> mostMeetings(List<Request> requests)`

One room only, and as many meetings as possible in it: the classic **activity selection**.
Take the requests by end (ties by start, then by id) and keep each one that starts at or after
the end of the last one kept. Return the kept requests in that order.

```text
a [0, 30)   b [10, 20)   c [20, 35)   d [30, 40)
by end: b, a, c, d → keep b, skip a (starts before 20), keep c, skip d (starts before 35): [b, c]
```

Both methods refuse a `null` list, a request with `start >= end` and a repeated id with an
`IllegalArgumentException`. An empty list gives `Plan(0, {})` and `[]`. Do not change the list
you receive.
