---
from: javascript
status: draft
concept: "Arrays and strings"
from_way: "Treating every array as a generic list of anything, and slicing strings freely."
native_way: "Plain arrays hold any values (V8 optimises arrays of small integers or doubles internally); typed arrays such as `Int32Array` and `Float64Array` are fixed-length, contiguous and primitive, like Java's int[]. Strings are immutable UTF-16, exactly like Java's."
sensei_tip: "`str.length` counts UTF-16 code units, so an emoji counts 2; `[...str].length` or `Array.from(str)` iterates code points. For a big numeric buffer, a typed array avoids boxing and is what WebGL, audio and WebAssembly use."
---

| JavaScript | Java | Note |
| --- | --- | --- |
| `[]` (Array) | `ArrayList<Object>` | dynamic, any values |
| `new Int32Array(n)` | `new int[n]` | fixed, contiguous, primitive |
| `arr.slice(i, j)` | `Arrays.copyOfRange` | copies |
| `str.length` | `s.length()` | UTF-16 units in both |
| `[...str]` | `s.codePoints()` | code points |
| `parts.join("")` | `String.join` | O(total length) |
| `str.localeCompare(other)` | `Collator.compare` | human-language order |
