---
from: java
status: draft
concept: "Arrays and strings"
from_way: "Reaching for List<Integer> and string concatenation everywhere, and comparing strings with ==."
native_way: "Use primitive arrays for large numeric data, Arrays and System.arraycopy for bulk work, StringBuilder for building text, and equals or compareTo for comparing it."
sensei_tip: "`Arrays.mismatch(a, b)` finds the first differing index of two arrays with vectorised code, and `String.chars()` / `codePoints()` give you an IntStream without allocating a char[]. For an anagram check on ASCII, `int[] count = new int[128]` beats any map."
---

| Java | Cost or note |
| --- | --- |
| `int[]` vs `Integer[]` | values in place vs references to boxed objects |
| `System.arraycopy`, `Arrays.copyOf` | bulk copy, O(n) but very fast |
| `Arrays.fill`, `Arrays.equals`, `Arrays.mismatch` | O(n) helpers |
| `String` | immutable, hash code cached |
| `s.substring(i, j)` | copies j − i chars |
| `StringBuilder.append` | amortised O(1) |
| `s.length()` vs `s.codePointCount(0, s.length())` | UTF-16 units vs code points |
| `s.toLowerCase(Locale.ROOT)` | locale-independent case |
