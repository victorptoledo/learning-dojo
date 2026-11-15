---
from: go
status: draft
concept: "Arrays and strings"
from_way: "Mixing up arrays and slices, and indexing strings as if each byte were a character."
native_way: "A Go array (`[8]int`) is a fixed-size value; a slice is a view over an array that can grow with append. A Go string is an immutable sequence of bytes, usually UTF-8: indexing gives bytes, ranging gives runes (code points)."
sensei_tip: "`for i, r := range s` walks code points; `s[i]` is a byte. `strings.Builder` is Go's StringBuilder, and slicing a string (`s[i:j]`) shares memory instead of copying, unlike Java's substring."
---

| Go | Java | Note |
| --- | --- | --- |
| `[n]int` | (no equivalent) | fixed-size array value |
| `[]int` | `int[]` + length, or `ArrayList` | slice: view with capacity |
| `copy(dst, src)` | `System.arraycopy` | bulk copy |
| `string` (immutable bytes, UTF-8) | `String` (UTF-16) | `len` counts bytes |
| `for _, r := range s` | `s.codePoints()` | runes = code points |
| `strings.Builder` | `StringBuilder` | amortised O(1) append |
| `s[i:j]` | `s.substring(i, j)` | Go shares, Java copies |
