Most of an office calendar repeats: the stand-up every day, the review every week, forever. A
series has no last occurrence, so it cannot be stored as a list of bookings: the scheduler keeps
the rule and computes occurrences when a question needs them.

Add to `Scheduler`:

```java
public boolean bookSeries(String id, String room, long firstStart, long duration, long every)
public List<Booking> upcoming(long from, int count)
```

**A series** books `[firstStart + n·every, firstStart + n·every + duration)` for every `n >= 0`.
`duration <= 0` or `every < duration` (a series that overlaps itself) is an
`IllegalArgumentException`, and so are a bad id or an unknown room, as in `book`. It returns
`false`, and changes nothing, when **any** occurrence, now or in a thousand years, would overlap a
booking or another series of the room. From now on `book` also returns `false` when the booking
overlaps an occurrence of a series.

**Occurrences** are bookings with the id `<series id>#<n>`, for example `standup#0`, `standup#1`.
`at`, `between`, `freeSlots` and `firstFreeEverywhere` see them like any other booking. Ids may not
contain `#` any more (an `IllegalArgumentException` in `book` and `bookSeries`). `cancel` of the
series id removes the whole series; `cancel("standup#3")` returns `false`. `size()` counts a series
as one.

**`upcoming(from, count)`** is the building's agenda: the next `count` bookings and occurrences of
every room that **start** at or after `from`, by start, ties by room name. A negative `count` is an
`IllegalArgumentException`.

```java
s.bookSeries("standup", "orion", 540, 15, 1_440);   // 9:00 every day
s.bookSeries("review", "orion", 600, 60, 10_080);   // 10:00 every week
s.book("lunch", "orion", 1_440 + 540, 1_440 + 600); // false: day 2's stand-up
s.at("orion", 3 * 1_440 + 545);                     // standup#3
s.upcoming(0, 3);                                   // standup#0, review#0, standup#1
```

**Clashes without listing occurrences.** A single booking `[s, e)` meets a series only through
the last occurrence that starts before `e`: `n = floorDiv(e - 1 - firstStart, every)`, when
`n >= 0`. Two series with periods `p` and `q` have starts that differ by every value of the form
`d + i·p - j·q`, where `d` is the difference of their first starts: these are exactly the numbers
`≡ d (mod gcd(p, q))` (Bézout's identity; with `i, j >= 0` too, because adding `lcm(p, q)` to both
keeps the difference). So they clash exactly when the smallest such value at or above zero is
less than the second series' duration, or the largest one below zero is more than minus the
first's: an O(log) check with Euclid's algorithm.

**The agenda is a merge.** Each series is an infinite sorted stream and each room's single
bookings are a sorted stream: put the first item of every stream in a min-heap and pop `count`
times, pushing the next item of the stream you popped. The tests have 2,000 series in 500 rooms and
ask for the agenda 2,000 times.
