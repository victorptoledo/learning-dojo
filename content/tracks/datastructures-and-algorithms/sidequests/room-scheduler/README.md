# Sidequest: Room scheduler

An office with hundreds of meeting rooms and a calendar that everyone uses at once. Each question
the calendar answers looks simple: is this room free at 10:00, which bookings fall in this
afternoon, how many rooms does tomorrow need, when is the first hour free in all of these rooms,
what comes next in the building. Each one has a slow answer (scan everything) and a fast one
(the right structure: a sorted map, a heap, a merge that only reads what it needs).

Build `dojo.rooms.Scheduler` and, in stage 2, `dojo.rooms.Planner`. The starter gives you the
contract records (`Booking`, and later `Request`, `Plan` and `Slot`) and the method signatures;
how you store and search is yours. The contract tests hold hundreds of thousands of bookings and
ask hundreds of thousands of questions under a time limit, so a linear scan per question fails.

- **Stage 1:** bookings per room, without overlaps, answered in O(log n).
- **Stage 2:** plan a day of meetings with the fewest rooms (sorting and two heaps), and pick the
  most meetings one room can hold (sorting and a greedy choice).
- **Stage 3:** free slots of a room, and the first slot free in a set of rooms, by merging their
  bookings lazily with a heap.
- **Stage 4:** weekly series that repeat forever, checked for clashes without listing their
  occurrences, and the building's agenda as a merge of every room and every series.

Times are `long` minutes and every interval is **half-open**, `[start, end)`: a meeting from 600
to 660 and one from 660 to 720 do not overlap. Use the JDK only: `TreeMap`, `PriorityQueue` and the
other collections are welcome. JUnit is there for your own tests.
