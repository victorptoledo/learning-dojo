---
from: javascript
status: draft
concept: "Numbers, strings, arrays and objects"
from_way: "One number type, undefined and null, arrays that grow with push, objects or Map for dictionaries, and loose conversions between strings and numbers."
native_way: "Sized integer and float types, zero values instead of undefined, slices grown with append, maps for dictionaries, and every conversion written out."
sensei_tip: "There is no undefined in Go: a missing map key gives the zero value. Use the comma-ok form whenever 0 or \"\" could be a real value."
---

| JavaScript | Go |
| --- | --- |
| `let n = 1.5` | `n := 1.5` (`float64`), `i := 1` (`int`) |
| `undefined` | the zero value |
| `const xs = []; xs.push(1)` | `var xs []int; xs = append(xs, 1)` |
| `xs.slice(1, 3)` (a copy) | `xs[1:3]` (shares memory) |
| `new Map()` / `{}` | `map[string]int{}` |
| `m.has(k)` | `_, ok := m[k]` |
| `"" + n` / `Number(s)` | `strconv.Itoa(n)` / `strconv.Atoi(s)` |
| `for (const x of xs)` | `for _, x := range xs` |
