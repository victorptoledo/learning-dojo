An editor searches the document all the time: find next, replace all, highlight every match. And
it shows line numbers, which means turning a position into a line and a line into a position.

Add to `TextBuffer`:

| Method | Does |
| --- | --- |
| `int find(String pattern, int from)` | The first index `>= from` where `pattern` starts, or `-1`. `from` outside `0..length()`, or an empty or `null` pattern, is an `IllegalArgumentException`. |
| `int replaceAll(String target, String replacement)` | Replaces every match of `target`, left to right and without overlaps (`"aaaa"` with `"aa"` has two), and returns how many. Bad arguments as in `find`; `replacement` may be empty. |
| `int lineCount()` | Lines are separated by `\n`: `""` has 1 line, `"a\nb\n"` has 3 (the last one empty). |
| `int lineOf(int position)` | The line (from 0) holding `position`, `0..length()`. Outside is an `IllegalArgumentException`. |
| `int lineStart(int line)` | The position where `line` starts. Outside `0..lineCount()-1` is an `IndexOutOfBoundsException`. |

**`find` in linear time, on the buffer itself.** Searching must cost O(distance to the match +
pattern length): a naive search compares the pattern again at every position and is O(n·m) on
text like `aaaa…a`, and copying the whole buffer into a `String` for each search pays for the
megabyte every time. Use the KMP prefix function (or the Z-function, or Rabin-Karp with
verification) over `charAt`.

**`replaceAll` is one step.** A `replaceAll` that replaced something is a single undo step:
`undo` brings the whole text back, with the cursor where it was before; `redo` reapplies it.
After the replacement, and after its redo, the cursor is at the end of the last replacement.
When nothing matched, nothing changes: no step, and redo keeps what it had.

**The line index.** Counting line breaks from the top on every call is O(n) per question; the
tests ask 200,000 questions about a megabyte of text. Keep the line starts in an array, built
when first needed after a change, and answer `lineOf` with a binary search
(`Arrays.binarySearch` is fine).

```java
var buffer = new TextBuffer("one fish\ntwo fish\n");
buffer.find("fish", 0);            // 4
buffer.find("fish", 5);            // 13
buffer.lineCount();                // 3
buffer.lineOf(13);                 // 1
buffer.lineStart(2);               // 18
buffer.replaceAll("fish", "cat");  // 2: "one cat\ntwo cat\n", cursor 15
buffer.undo();                     // back to fish
```
