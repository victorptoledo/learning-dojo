# Implicit Interfaces, Embedding and Type Switches

An interface in Go is a **set of methods**. A type satisfies it by having those methods;
there is no `implements` keyword. This one rule, structural typing checked at compile time,
shapes how Go programs are designed: small interfaces, defined where they are used.

## Declaring and satisfying

```go
type Shape interface {
	Area() float64
	Perimeter() float64
}

type Rect struct{ W, H float64 }

func (r Rect) Area() float64      { return r.W * r.H }
func (r Rect) Perimeter() float64 { return 2 * (r.W + r.H) }

type Circle struct{ R float64 }

func (c Circle) Area() float64      { return math.Pi * c.R * c.R }
func (c Circle) Perimeter() float64 { return 2 * math.Pi * c.R }

func Total(shapes []Shape) (sum float64) {
	for _, s := range shapes {
		sum += s.Area()
	}
	return sum
}

Total([]Shape{Rect{3, 4}, Circle{1}})
```

`Rect` never mentions `Shape`. If you want the compiler to confirm that a type satisfies an
interface, add a line that costs nothing at run time:

```go
var _ Shape = Rect{}
var _ io.Writer = (*Buffer)(nil)
```

### Method sets

If a method has a **pointer receiver**, only the **pointer** type has it. With
`func (b *Buffer) Write(p []byte) (int, error)`, a `*Buffer` is an `io.Writer` and a `Buffer`
value is not. The compiler tells you: "Buffer does not implement io.Writer (method Write has
pointer receiver)".

## Small interfaces

The standard library is built on interfaces of **one or two methods**:

| Interface | Method | Used by |
| --- | --- | --- |
| `error` | `Error() string` | every fallible function |
| `fmt.Stringer` | `String() string` | `fmt.Print` and friends |
| `io.Reader` | `Read(p []byte) (n int, err error)` | files, sockets, HTTP bodies, `strings.Reader`, gzip |
| `io.Writer` | `Write(p []byte) (n int, err error)` | files, `os.Stdout`, `bytes.Buffer`, HTTP responses, hashes |
| `io.Closer` | `Close() error` | anything to release |
| `sort.Interface` | `Len`, `Less`, `Swap` | `sort.Sort` |
| `http.Handler` | `ServeHTTP(ResponseWriter, *Request)` | every HTTP server |

Because they are tiny, everything fits together: `io.Copy(dst Writer, src Reader)` copies a
file to a socket, an HTTP body to a hash, or a gzip stream to standard output.

```go
func CountLines(r io.Reader) (int, error) {
	scanner := bufio.NewScanner(r)
	n := 0
	for scanner.Scan() {
		n++
	}
	return n, scanner.Err()
}

CountLines(strings.NewReader("a\nb\n"))  // in tests
CountLines(file)                         // in production
```

**Accept interfaces, return concrete types.** A function that only reads should accept an
`io.Reader`, not an `*os.File`; a constructor returns `*Store`, not a `StoreInterface`.

**Define interfaces where they are used**, by the consumer, with only the methods it needs.
The package that implements a type does not need to export an interface for it; callers can
declare their own `type userGetter interface { Get(id string) (User, error) }` and pass the
real store or a fake in tests.

## Composing interfaces

Interfaces embed other interfaces:

```go
type ReadWriter interface {
	Reader
	Writer
}

type ReadCloser interface {
	Reader
	Closer
}
```

A struct can embed an interface value or a concrete type to **decorate** it: it gets all the
methods and overrides only the ones it changes.

```go
type countingWriter struct {
	io.Writer        // every method of the wrapped writer is promoted
	bytes int
}

func (c *countingWriter) Write(p []byte) (int, error) {
	n, err := c.Writer.Write(p)
	c.bytes += n
	return n, err
}
```

## Interface values, nil and the typed nil trap

An interface value holds two things: a **dynamic type** and a **dynamic value**. It is `nil`
only when **both** are missing.

```go
func find(id string) error {
	var err *NotFoundError        // a nil pointer of a concrete type
	if id == "" {
		err = &NotFoundError{}
	}
	return err                    // BUG: returns a non-nil error holding (*NotFoundError, nil)
}

if find("x") != nil { ... }       // true, even though no error happened
```

Return the literal `nil` for "no error" and only assign concrete error types when you have one.

## Type assertions and type switches

`any` (an alias of `interface{}`) accepts every value. To get a concrete type back:

```go
var v any = "hello"

s := v.(string)          // panics if v is not a string
n, ok := v.(int)         // comma ok: n == 0, ok == false, no panic

switch x := v.(type) {   // a type switch
case string:
	fmt.Println("string of", len(x))
case int, int64:
	fmt.Println("an integer", x)     // x is still any here: two types in one case
case fmt.Stringer:
	fmt.Println(x.String())
case nil:
	fmt.Println("nothing")
default:
	fmt.Printf("%T\n", x)
}
```

You can also assert to an **interface**, to ask about an optional capability:
`if f, ok := w.(interface{ Flush() error }); ok { f.Flush() }`. That is how `io.Copy` uses a
faster `WriterTo` when the source has one. For errors, prefer `errors.As`, which also looks
inside wrapped errors.

Use `any` at real boundaries (decoding unknown JSON, a generic container before generics,
`fmt`), not to avoid designing types. The next lesson, on generics, removes most of the
remaining reasons.

## Summary

- A type satisfies an interface by having its methods; check it with `var _ I = T{}`.
- Pointer receivers put the method on `*T` only.
- Keep interfaces small, define them at the consumer, accept interfaces and return structs.
- `io.Reader` and `io.Writer` make files, buffers, sockets and HTTP bodies interchangeable.
- An interface is nil only when its type and value are both nil: beware the typed nil.
- Type assertions (with comma ok) and type switches recover concrete types.

## Further reading

- A Tour of Go, "Interfaces": <https://go.dev/tour/methods/9>
- Effective Go, "Interfaces and other types": <https://go.dev/doc/effective_go#interfaces_and_types>
- The Go spec, "Interface types" and "Type switches": <https://go.dev/ref/spec#Interface_types>
- Go FAQ, "Why is my nil error value not equal to nil?": <https://go.dev/doc/faq#nil_error>
- Package io: <https://pkg.go.dev/io>
- roadmap.sh, Go topic "Interfaces Basics": <https://github.com/nilbuild/developer-roadmap/blob/master/roadmaps/golang/content/interfaces-basics@nkONsPlFYCMqzWHTm_EPB.md>
