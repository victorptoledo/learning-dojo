The calendar's basic job: keep each room's bookings, refuse the ones that clash, and answer
"what is on" quickly.

Fill in `Scheduler`:

| Method | Does |
| --- | --- |
| `void addRoom(String room)` | Adds a room. A `null`, blank or already added name is an `IllegalArgumentException`. |
| `boolean book(String id, String room, long start, long end)` | Books `[start, end)`. Returns `false`, and changes nothing, when it overlaps another booking of the room. A `null` or blank id, an id already booked, an unknown room or `start >= end` is an `IllegalArgumentException`. |
| `boolean cancel(String id)` | Removes the booking; `false` when there is none. Its id may be booked again. |
| `Optional<Booking> at(String room, long time)` | The booking of the room with `start <= time < end`, if any. |
| `List<Booking> between(String room, long from, long to)` | The bookings of the room that overlap `[from, to)`, by start. `from == to` gives an empty list, `from > to` is an `IllegalArgumentException`. |
| `int size()` | Bookings in all rooms. |

An unknown room is an `IllegalArgumentException` in `at` and `between` too.

```java
var s = new Scheduler();
s.addRoom("orion");
s.book("standup", "orion", 540, 555);     // true
s.book("review", "orion", 550, 600);      // false: 550 is inside the stand-up
s.book("review", "orion", 555, 600);      // true: they touch, they do not overlap
s.at("orion", 554);                       // Optional[Booking[id=standup, room=orion, start=540, end=555]]
s.between("orion", 0, 541);               // [standup]
```

The bookings of one room never overlap, so ordering them by start also orders them by end. A
sorted map from start to booking answers each question with one or two searches: the booking
that contains a time is the last one that starts at or before it (`floorEntry`), and the bookings
of a window start from there. The tests book 200,000 meetings in one room in random order and ask
200,000 questions: a list scanned from the top is too slow.
