# Checkpoint 4: Files, Time and Text

This checkpoint closes the **Files, Time and Text** phase. It has two parts, and you need both:

1. **The quiz** (press **Q**): eight questions over the whole phase. You need 7 right.
2. **The integration kata** (press **Enter**): a backup rotator. `scan` lists a folder with
   `std::filesystem` and error codes and reads the local time in each file name, in a real
   time zone where some times happen twice and some never; `plan` keeps the newest backup of
   each recent day, ISO week and month; `report` lines it up with `std::format`; and `apply`
   removes the rest and replaces its log atomically.

Do them in any order. Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| Formatting and Parsing Text | Did `from_chars` consume the whole field? How do I pad to a width known only at run time? |
| Files, Streams and std::filesystem | Is this failure expected (error code) or fatal (exception)? Am I checking before acting? How do I replace a file so nobody sees half of it? |
| Time with std::chrono | Is this a local time or UTC? What happens to it on the nights the clocks change? Which year does an ISO week belong to? |
| Durations and Timestamps (kata) | Which fields can be out of range, and does `ok()` catch them? |

> [!TIP]
> If a quiz question surprises you, open the lesson it comes from again before retrying. The
> explanation after each answer says which idea it tests.
