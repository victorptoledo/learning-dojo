---
from: python
status: draft
concept: "Arrays and strings"
from_way: "Using lists of Python ints for everything and building strings with += in a loop."
native_way: "A list holds references to objects, like Java's ArrayList<Integer>; the `array` module and NumPy hold raw numbers, like int[]. Strings are immutable sequences of code points, so `len` counts characters, not UTF-16 units."
sensei_tip: "`''.join(parts)` is the StringBuilder of Python, and `collections.Counter(s)` is the one-line frequency count. `itertools.accumulate` builds prefix sums; NumPy's `cumsum` does it for a whole column at C speed."
---

| Python | Java | Note |
| --- | --- | --- |
| `list` of ints | `ArrayList<Integer>` | references to objects |
| `array.array('i')`, NumPy array | `int[]` | raw values, contiguous |
| `str` (immutable, code points) | `String` (immutable, UTF-16) | `len` counts code points |
| `''.join(parts)` | `String.join` / `StringBuilder` | O(total length) |
| `s[i:j]` | `s.substring(i, j)` | copies |
| `Counter(s)` | `int[]` counts or `HashMap` | O(n) |
| `itertools.accumulate(a)` | prefix-sum loop | O(n) |
