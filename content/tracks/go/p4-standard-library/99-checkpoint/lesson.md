# Checkpoint 4: The Standard Library

This checkpoint closes **The Standard Library** phase. It has two parts, and you need both:

1. **The quiz** (press **Q**): eight questions over the whole phase. You need 7 right.
2. **The integration kata** (press **Enter**): a meter readings import. It reads CSV files
   from an `fs.FS` (columns in any order, a byte order mark, bad rows that must not stop the
   file), turns local times and RFC 3339 instants into one kind of value, adds up energy per
   local day in Lisbon across both daylight saving changes, and writes a JSON report with a
   temporary file and a rename.

Do them in any order. Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| Files, io and bufio | Why accept an `fs.FS` or an `io.Reader`? Which error of `Close` matters? How do I replace a file so nobody reads half of it? |
| Encoding | Which struct tag leaves a zero value out? When do I write `MarshalText` instead of `MarshalJSON`? What does `csv.Reader` do with a row of the wrong length? |
| Time | What is the reference time? What does `ParseInLocation` do with a time that does not exist? Why is a day not always 24 hours? |
| Strict Config | How do I reject unknown fields and trailing data? How do I tell "missing" from "zero"? |

> [!TIP]
> The kata's dates are real: in 2026, Lisbon moves its clocks forward at 01:00 on 29 March
> and back at 02:00 on 25 October. Write the times you test down in UTC next to the local
> ones before you write code.
