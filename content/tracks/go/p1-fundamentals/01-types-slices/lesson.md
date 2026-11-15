# Types, Zero Values, Strings, Slices and Maps

Go is statically typed with a small set of built-in types. This lesson covers declaring
values, the basic types, control flow, and the two collection types you use every day:
**slices** and **maps**, including how they work inside, because most Go surprises come from
there.

## Declaring values

```go
var count int          // declared, holds the zero value 0
var name = "Ada"       // type inferred: string
total := 42            // short declaration, only inside functions
const Pi = 3.14159     // constant, computed at compile time

a, b := 1, 2
a, b = b, a            // swap: assignments are parallel
```

Every type has a **zero value**, and a variable always holds a valid value: `0` for numbers,
`""` for strings, `false` for booleans, and `nil` for pointers, slices, maps, channels,
functions and interfaces. Good Go types are designed so that **the zero value is useful**
(`var b strings.Builder` or `var mu sync.Mutex` work without a constructor).

`iota` numbers constants in a block:

```go
type Weekday int

const (
	Sunday Weekday = iota // 0
	Monday                // 1
	Tuesday               // 2
)
```

## Basic types and conversions

| Kind | Types |
| --- | --- |
| Integers | `int`, `int8` … `int64`, `uint`, `uint8` … `uint64`, `uintptr` (`int` is 64-bit on 64-bit platforms) |
| Floats, complex | `float32`, `float64`, `complex64`, `complex128` |
| Text | `string` (immutable bytes, usually UTF-8), `byte` (= `uint8`), `rune` (= `int32`, a Unicode code point) |
| Other | `bool` |

There are **no implicit conversions**, not even from `int` to `int64` or `float64`:

```go
var n int = 3
var x float64 = float64(n) / 2   // 1.5; n / 2 would be integer division
s := strconv.Itoa(n)             // "3"; string(n) would be the rune U+0003
```

### Strings, bytes and runes

A string is a read-only slice of **bytes**. `len(s)` counts bytes, `s[i]` is a byte, and
`range` decodes **runes**:

```go
s := "héllo"
fmt.Println(len(s))                    // 6: é takes two bytes in UTF-8
fmt.Println(utf8.RuneCountInString(s)) // 5
for i, r := range s {
	fmt.Println(i, string(r))          // 0 h, 1 é, 3 l, 4 l, 5 o
}
```

Build strings with `strings.Builder` (or `fmt.Sprintf`), not with `+=` in a loop. The
`strings` package has what you expect: `Fields`, `Split`, `Cut`, `Contains`, `TrimSpace`,
`ToLower`, `Repeat`, `ReplaceAll`, and since Go 1.27 `CutLast`.

## Control flow

```go
if v, err := strconv.Atoi(input); err == nil {   // statement before the condition
	fmt.Println(v)
}

for i := 0; i < 3; i++ { }    // the only loop keyword is for
for n < 100 { n *= 2 }        // "while"
for i := range 10 { }         // range over an integer (Go 1.22)
for { break }                 // forever

switch day {
case Saturday, Sunday:
	fmt.Println("weekend")    // no fallthrough by default
default:
	fmt.Println("weekday")
}
```

Since Go 1.22, each loop iteration has **its own** loop variable, so closures and goroutines
started in a loop no longer all see the last value.

## Arrays and slices

An **array** has a fixed length that is part of its type (`[3]int`), and it is a **value**:
assigning or passing it copies every element. You rarely use arrays directly.

A **slice** is a small descriptor (a *slice header*) of three words that points into an
array:

```text
s := []int{10, 20, 30, 40}       t := s[1:3]

 s: ptr ─┐ len 4 cap 4          t: ptr ─────┐ len 2 cap 3
         ▼                                  ▼
       [ 10 | 20 | 30 | 40 ]    (the same backing array)
```

- `len(s)` is how many elements the slice shows, `cap(s)` how many fit before the end of the
  backing array.
- Slicing (`s[1:3]`) **shares** the backing array: writing `t[0] = 99` changes `s[1]`.
- `append` writes into the free capacity if there is some, otherwise it allocates a bigger
  array (roughly doubling for small slices), copies, and returns a slice of the new array.
  **Always use the result**: `s = append(s, x)`.

```go
a := make([]int, 3, 10)  // len 3, cap 10
b := append(a, 1)        // fits: b shares a's array
c := append(a, 2)        // also fits: overwrites b[3]! b[3] is now 2
```

That last line is the classic aliasing bug. When you hand out a sub-slice that callers may
append to, limit its capacity with a full slice expression `s[low:high:max]`, or copy it with
`slices.Clone(s)`. To keep a small part of a huge slice, copy it, or the big array stays
alive.

A `nil` slice has length 0 and works with `len`, `range` and `append`, so
`var result []string` is the normal way to start. Preallocate with `make([]T, 0, n)` when
you know the size.

The `slices` package (generic, Go 1.21+) covers the common operations: `slices.Sort`,
`slices.SortFunc`, `slices.Contains`, `slices.Index`, `slices.Reverse`, `slices.Max`,
`slices.Equal`, `slices.Compact`, `slices.Insert`, `slices.Delete`.

## Maps

A map is a hash table from comparable keys to values:

```go
ages := map[string]int{"ada": 36}
ages["alan"] = 41
age, ok := ages["grace"]   // the "comma ok" form: 0, false when missing
delete(ages, "alan")
for name, age := range ages { }  // the order is random on purpose
```

- Reading a missing key returns the **zero value**, which makes counting easy:
  `counts[word]++`.
- A `nil` map can be read but **writing to it panics**: create maps with a literal or `make`.
- **Iteration order is randomised**. To print in order, collect the keys and sort them:
  `slices.Sorted(maps.Keys(m))`.
- Maps are references: passing a map to a function lets it change the caller's map.
- A map is **not safe for concurrent writes**; the runtime may stop the program with
  "concurrent map writes" (the concurrency phase shows how to protect it).
- Since Go 1.24, maps are implemented as **Swiss tables**, which made them faster; the
  behaviour above is unchanged.

Keys must be comparable: numbers, strings, booleans, pointers, channels, arrays and structs
of comparable fields. Slices, maps and functions cannot be keys.

## Summary

- `:=` inside functions, `var` for zero values, `const` and `iota` for constants.
- Every type has a usable zero value; conversions are always explicit.
- Strings are immutable bytes; `range` over a string yields runes.
- A slice is pointer, length and capacity over a shared array; always keep `append`'s result
  and watch out for aliasing.
- Maps return the zero value for missing keys, panic when written while nil, and iterate in
  random order.

## Further reading

- A Tour of Go, "More types: structs, slices, and maps": <https://go.dev/tour/moretypes/1>
- Go Slices: usage and internals (Go blog): <https://go.dev/blog/slices-intro>
- Strings, bytes, runes and characters in Go (Go blog): <https://go.dev/blog/strings>
- Faster Go maps with Swiss Tables (Go blog): <https://go.dev/blog/swisstable>
- The Go spec, "Slice types" and "Map types": <https://go.dev/ref/spec#Slice_types>
- Effective Go, "Data": <https://go.dev/doc/effective_go#data>
- roadmap.sh, Go topic "Capacity and growth": <https://github.com/nilbuild/developer-roadmap/blob/master/roadmaps/golang/content/capacity-and-growth@qUykEUH1-9Dzotc_ltV3g.md>
