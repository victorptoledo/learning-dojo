# Log Index

An incident tool keeps a log that grows all day and whose lines are sometimes rewritten
(a redaction, a late correction). Operators **watch** patterns such as `ERROR` or `user=42`, and
ask range questions over line numbers: how many matches between line 120,000 and line 180,000,
where is the next line with a match, and which line of a range has the most.

```java
var log = new LogIndex();
log.watch("ERROR");
log.append("09:00 INFO start");                // line 0
log.append("09:01 ERROR disk ERROR retry");    // line 1
log.append("09:02 INFO ok");                   // line 2
log.append("09:03 ERROR net");                 // line 3
log.watch("aa");                               // counts the lines already there too
log.append("aaaa");                            // line 4: "aa" occurs 3 times
log.count("ERROR", 0, 5);        // 3
log.count("aa", 0, 5);           // 3
log.firstLineWith("ERROR", 2);   // 3
log.busiestLine("ERROR", 0, 5);  // 1
log.replace(1, "09:01 INFO fixed");
log.busiestLine("ERROR", 0, 5);  // 3
```

## What to write

| Method | Does |
| --- | --- |
| `watch(pattern)` | starts counting a non-empty pattern in every line, the lines already there included; watching it again does nothing |
| `append(line)` | adds a line at the end and returns its number (0, 1, 2, …) |
| `replace(number, line)` | replaces the text of an existing line |
| `lines()`, `line(number)` | the number of lines, the text of a line |
| `count(pattern, from, to)` | the occurrences of a watched pattern in the lines `[from, to)`; occurrences **overlap** (`aa` occurs 3 times in `aaaa`) and never cross a line |
| `firstLineWith(pattern, from)` | the first line at or after `from` with at least one occurrence, or -1 |
| `busiestLine(pattern, from, to)` | the line of `[from, to)` with the most occurrences, the **first** one on ties |

## Errors

- A `null` or empty pattern in `watch`, a pattern that is not watched in a question, an empty
  range in `busiestLine`, and a line that contains a line break (`'\n'`) throw
  `IllegalArgumentException`.
- A line number outside the log, or a range with `from < 0`, `to > lines()` or `from > to`, throws
  `IndexOutOfBoundsException` (`from` may equal `lines()` in `firstLineWith`).
- A `null` line throws `NullPointerException`.
- A refused `append` or `replace` changes nothing.

## How it is graded

This is the integration kata of the Range Queries and Strings phase. The hidden tests compare
every answer with a slow model through thousands of random appends, replaces and late watches,
then time:

- 300,000 rounds of `count` and `busiestLine` over most of 200,000 lines, with a `replace` every
  third round (a loop over the range is too slow);
- 200,000 `firstLineWith` calls on 300,000 lines where only the last line matches (a scan is too
  slow);
- twelve lines of 150,000 `a`s against `a…a` and `a…ab` of 5,000 characters (restarting the search
  at every position is O(n · m));
- a million appends (rebuilding the trees on every append is too slow: grow them by doubling).

> Tip: each watched pattern needs its KMP prefix function, an array of counts per line, a Fenwick
> tree over the counts and a segment tree of positions over the same counts. When the log
> outgrows them, double the capacity and rebuild both trees in O(n).
