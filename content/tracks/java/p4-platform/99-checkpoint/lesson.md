# Checkpoint 4: The Java Platform

This checkpoint closes **The Java Platform** phase. It has two parts, and you need both:

1. **The quiz** (press **Q**): eight questions over the whole phase. You need 7 right.
2. **The integration kata** (press **Enter**): a timesheet import that reads an inbox of CSV
   files, maps each row to a record with an annotation-driven mapper, counts real time across
   daylight saving changes, groups by ISO week and writes its report atomically.

Do them in any order. Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| Files, Paths and Text I/O | Which streams must I close? In what order does `Files.list` return files? How do I replace a file so no reader sees half of it? |
| Dates and Times with java.time | Which type is a moment and which is what a person typed? What happens to a duration on the night the clocks change? Which year is an ISO week in? |
| Annotations and Reflection | Why can reflection see this annotation? What should I check once per class? Where is the real exception? |
| Record Validator (kata) | How did I walk the components and unwrap accessor errors? |

> [!TIP]
> The kata's dates are chosen on purpose: the two daylight saving nights of 2026 in Lisbon and
> the days around the new year. Work them out by hand before you trust your code.
