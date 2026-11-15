# Generics, Constraints and Iterators

Since Go 1.18, functions and types can take **type parameters**. Since Go 1.23, `range` can
loop over **functions**, which gave Go a standard iterator protocol. Together they replaced
most uses of `interface{}` in containers and algorithms, and Go 1.27 added **generic
methods**.

## Generic functions

```go
func Map[T, U any](xs []T, f func(T) U) []U {
	out := make([]U, 0, len(xs))
	for _, x := range xs {
		out = append(out, f(x))
	}
	return out
}

lengths := Map([]string{"go", "gopher"}, func(s string) int { return len(s) })  // [2 6]
```

The type parameters go in square brackets. Each one has a **constraint**, an interface that
says what the type must support. Usually the compiler **infers** the type arguments from the
values; you write them only when it cannot: `Map[string, int](nil, f)`.

## Constraints

| Constraint | Allows |
| --- | --- |
| `any` | every type; you can only assign, pass and compare with nil where allowed |
| `comparable` | types that support `==` and `!=` (map keys) |
| `cmp.Ordered` | integers, floats and strings: `<`, `>` and `cmp.Compare` |
| your own type set | exactly the types listed |

A constraint can list types. `~T` means "any type whose **underlying type** is T", so your
own named types are accepted too:

```go
type Number interface {
	~int | ~int64 | ~float64
}

func Sum[N Number](xs ...N) N {
	var total N
	for _, x := range xs {
		total += x
	}
	return total
}

type Cents int64
Sum(Cents(150), Cents(250))   // Cents(400): ~int64 accepts Cents
```

A constraint can also require methods (`interface{ ~int; String() string }`), and since Go
1.26 a generic type can refer to itself in its own constraint, as in
`type Adder[A Adder[A]] interface{ Add(A) A }`.

## Generic types

```go
type Stack[T any] struct {
	items []T
}

func (s *Stack[T]) Push(v T) { s.items = append(s.items, v) }

func (s *Stack[T]) Pop() (T, bool) {
	var zero T                       // the zero value of whatever T is
	if len(s.items) == 0 {
		return zero, false
	}
	v := s.items[len(s.items)-1]
	s.items = s.items[:len(s.items)-1]
	return v, true
}

var s Stack[string]     // the zero value works: a nil slice is fine
s.Push("a")
```

Since Go 1.24, aliases can have type parameters: `type Set[T comparable] = map[T]struct{}`.

### Generic methods (Go 1.27)

Until Go 1.26 a method could only use its type's parameters. Since Go 1.27 a method may
declare **its own** type parameters:

```go
func (s *Stack[T]) Map[U any](f func(T) U) *Stack[U] {
	out := &Stack[U]{items: make([]U, 0, len(s.items))}
	for _, v := range s.items {
		out.items = append(out.items, f(v))
	}
	return out
}
```

Two limits remain: an **interface** method cannot have type parameters, and a generic method
therefore never satisfies an interface method. `math/rand/v2` uses the feature for
`(*Rand).N`.

### When to use generics

Use them for **containers** (sets, caches, queues), **algorithms over any element type**
(sort, map, filter, min) and to remove duplicated code that differs only by type. Do not use
them when an interface with methods says what you need (`io.Reader` is better than
`[T Reader]`), or when there is only one type in sight. Write the concrete version first;
generalise when the second type arrives.

## Iterators: range over functions

An **iterator** is a function that calls a `yield` function once per value:

```go
// package iter
type Seq[V any] func(yield func(V) bool)
type Seq2[K, V any] func(yield func(K, V) bool)
```

`yield` returns `false` when the loop body wants to stop (a `break` or a `return`). The
iterator **must** then stop calling it; if it does not, the program panics.

```go
func Countdown(from int) iter.Seq[int] {
	return func(yield func(int) bool) {
		for i := from; i >= 0; i-- {
			if !yield(i) {
				return        // the consumer broke out of the loop
			}
		}
	}
}

for n := range Countdown(3) {   // 3 2 1 0
	fmt.Println(n)
}
```

Iterators are **lazy**: nothing runs until someone ranges over them, and they can be
infinite. They compose:

```go
func Filter[T any](seq iter.Seq[T], keep func(T) bool) iter.Seq[T] {
	return func(yield func(T) bool) {
		for v := range seq {
			if keep(v) && !yield(v) {
				return
			}
		}
	}
}
```

The standard library speaks iterators:

| Function | Gives |
| --- | --- |
| `slices.All(s)` / `slices.Values(s)` | index and value / values of a slice |
| `slices.Collect(seq)` | a new slice from a `Seq` |
| `slices.Sorted(seq)` | a sorted slice from a `Seq` |
| `maps.Keys(m)` / `maps.Values(m)` / `maps.All(m)` | the parts of a map |
| `strings.SplitSeq`, `strings.FieldsSeq`, `strings.Lines` | pieces of a string without building a slice |
| `iter.Pull(seq)` | turns a push iterator into `next()` and `stop()` functions |

Return an `iter.Seq` from your own collections (a `func (s *Set[T]) All() iter.Seq[T]`
method) instead of exposing the internal slice or map.

## Summary

- Type parameters in brackets, constrained by interfaces: `any`, `comparable`, `cmp.Ordered`
  or type sets with `~`.
- Type arguments are usually inferred; the zero value of `T` is `var zero T`.
- Generic types and, since Go 1.27, generic methods; interface methods still cannot be
  generic.
- Iterators are `func(yield func(V) bool)`; stop when yield returns false. Use `iter.Seq`,
  `slices.Collect`, `maps.Keys` and friends.

## Further reading

- Tutorial, "Getting started with generics": <https://go.dev/doc/tutorial/generics>
- An Introduction To Generics (Go blog): <https://go.dev/blog/intro-generics>
- When To Use Generics (Go blog): <https://go.dev/blog/when-generics>
- Range Over Function Types (Go blog): <https://go.dev/blog/range-functions>
- Package iter: <https://pkg.go.dev/iter>
- Go 1.27 release notes, generic methods: <https://go.dev/doc/go1.27>
- roadmap.sh, Go topic "Type Constraints": <https://github.com/nilbuild/developer-roadmap/blob/master/roadmaps/golang/content/type-constraints@8o6A2kGnupGYaaTwgsTDp.md>
