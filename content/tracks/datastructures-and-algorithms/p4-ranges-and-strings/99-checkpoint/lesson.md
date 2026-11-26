# Checkpoint 4: Range Queries and Strings

This checkpoint closes the **Range Queries and Strings** phase. It has two parts, and you need
both:

1. **The quiz** (press **Q**): eight questions over the whole phase. You need 7 right.
2. **The integration kata** (press **Enter**): an editable log index. It counts watched patterns
   in each line with KMP (overlapping matches, no restart on repetitive lines), keeps the counts
   in a Fenwick tree for range sums and "the next line with a match" (a search by prefix), and in
   a segment tree for "the busiest line of a range", all while lines are appended and rewritten.

Do them in any order. Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| Range Queries | Does the data change? Does the operation have an inverse, or does it tolerate overlap? Which structure follows from those two answers? |
| Range Toolkit (kata) | Why must the Fenwick array be 1-based? How does the descent over powers of two find a prefix? Which position wins a tie in the segment tree? |
| String Matching | What is the worst case of the naive search? Where does KMP resume after a full match? When do I verify a hash? |
| Text Search (kata) | How does the prefix function give a period? Why does a binary search on the length work for repeated substrings? |

> [!TIP]
> A line number is a position in an array of counts, one array per watched pattern. "Matches
> between two lines" is then a range sum, "the next line with a match" is the first position
> where the running total grows, and "the busiest line" is a range argmax.
