A gap buffer is an array with a hole in it. The text before the cursor sits at the start of the
array, the text after it sits at the end, and the free space between them is the **gap**:

```text
"hello world", cursor after "hello"
[h e l l o _ _ _ _ _ _   w o r l d]
          ^gapStart     ^gapEnd
```

Typing writes into the gap and shrinks it: O(1). Deleting next to the cursor just widens the
gap: O(1). Moving the cursor copies the characters it passes over to the other side of the gap:
O(distance), which is small because people edit near where they are. When the gap is full,
grow the array (double it, as a dynamic array does), so typing stays O(1) amortised.

Fill in `TextBuffer`:

| Method | Does |
| --- | --- |
| `TextBuffer()`, `TextBuffer(String initial)` | An empty buffer, or one holding `initial` with the cursor at its **end**. `null` is an `IllegalArgumentException`. |
| `int length()`, `int cursor()` | The number of characters, and the cursor position (`0..length()`). O(1). |
| `void moveTo(int position)` | Moves the cursor. Outside `0..length()` is an `IllegalArgumentException`. |
| `void insert(String text)` | Inserts at the cursor; the cursor ends up after the new text. `""` changes nothing, `null` is an `IllegalArgumentException`. |
| `int deleteBackward(int count)` | Backspace `count` times: removes up to `count` characters before the cursor and returns how many it removed (fewer at the start of the text). |
| `int deleteForward(int count)` | The Delete key: removes up to `count` characters after the cursor, which does not move. |
| `char charAt(int index)` | The character at `index` (`0..length()-1`), O(1). Outside is an `IndexOutOfBoundsException`. |
| `String substring(int from, int to)` | The text in `[from, to)`. Bad bounds are an `IndexOutOfBoundsException`. |
| `String text()` | The whole text. |

A negative `count` is an `IllegalArgumentException`.

```java
var buffer = new TextBuffer("hello world");
buffer.moveTo(5);
buffer.insert(",");          // "hello, world", cursor 6
buffer.deleteForward(1);     // "hello,world"
buffer.deleteBackward(99);   // returns 6, "world", cursor 0
```

The tests type 300,000 characters one at a time with two million characters after the cursor,
jump the cursor around a document of 600,000 characters, and read it back with `charAt`: a
`String` or a `StringBuilder` edited in the middle cannot finish in time.
