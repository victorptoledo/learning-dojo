People do not want a booking, they want a time: "when is the first free hour in all three rooms
on the third floor?" The starter adds `Slot(long start, long end)`; add to `Scheduler`:

| Method | Does |
| --- | --- |
| `List<Slot> freeSlots(String room, long from, long to)` | The free time of the room inside `[from, to)`, as maximal slots in order. `from == to` gives `[]`; `from > to` or an unknown room is an `IllegalArgumentException`. |
| `Optional<Slot> firstFreeEverywhere(List<String> rooms, long from, long to, long length)` | The earliest `[t, t + length)` inside `[from, to)` during which **every** room of the list is free, or empty when there is none. A `null` or empty list, an unknown room, `length <= 0` or `from > to` is an `IllegalArgumentException`. |

```java
// orion: [540, 555) [600, 660)      lyra: [500, 570) [640, 700)
s.freeSlots("orion", 500, 700);                                   // [500, 540) [555, 600) [660, 700)
s.firstFreeEverywhere(List.of("orion", "lyra"), 500, 800, 30);    // [570, 600)
s.firstFreeEverywhere(List.of("orion", "lyra"), 500, 800, 31);    // [700, 731)
s.firstFreeEverywhere(List.of("orion", "lyra"), 500, 720, 31);    // empty
```

**A lazy k-way merge.** Each room already has its bookings sorted. Put the first booking of each
room that ends after `from` in a min-heap ordered by start, and keep a candidate `t`, starting at
`from`. Pop the booking that starts first: if it starts at or after `t + length`, the slot
`[t, t + length)` is free in every room, so stop; otherwise the slot cannot start before that
booking ends, so move `t` to `max(t, end)` and push the next booking of the same room. When `t +
length` passes `to`, there is no slot.

It costs O(k log k) to start and O(log k) per booking it reads, and it reads only the bookings
before the answer. The tests ask 2,000 questions about 1,000 rooms with 200 bookings each, where
the answer is near `from`: collecting and sorting every booking of the window for each question is
far too slow.
