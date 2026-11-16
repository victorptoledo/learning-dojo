# Readings Report

Practice for *Checkpoint 2*. A sensor writes one temperature per line. Implement the
pieces marked `todo!` in `src/lib.rs`.

1. `parse_readings(text)` reads one number per line, trimming spaces and skipping blank
   lines. The first line that is not a number stops it with a `ParseError` holding the
   line number (counted from 1, blank lines included) and the trimmed text.
2. `Display` for `ParseError`: ``line 2: `abc` is not a number``.
3. `Readings::max` and `Readings::mean` return `None` when there are no readings.
   Use iterator methods, not index loops.
4. `Summary` for `Readings`: `3 readings, max 21.5, mean 20.0` (one decimal),
   `1 reading, max 4.0, mean 4.0` for a single one, and `no readings` when empty.

Save a file to run the checks. Passing gives practice XP; it does not change the checkpoint.
