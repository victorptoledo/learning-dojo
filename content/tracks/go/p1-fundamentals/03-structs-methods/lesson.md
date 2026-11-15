# Structs, Methods, Pointers and Embedding

Go has no classes. It has **structs** for data, **methods** you can attach to any named
type, **pointers** to share and mutate values, and **embedding** to compose types. There is
no inheritance, no constructor keyword and no `this`: just these few pieces.

## Structs

```go
type Account struct {
	ID      string
	Owner   string
	Balance int64 // in cents: never use float64 for money
	tags    []string
}

a := Account{ID: "acc-1", Owner: "Ada"}   // named fields; the rest get zero values
b := Account{"acc-2", "Alan", 0, nil}      // positional: fragile, avoid outside tiny types
var c Account                              // every field at its zero value
p := &Account{ID: "acc-3"}                 // a pointer to a new struct
p.Balance = 500                            // same as (*p).Balance: no -> operator
```

- Upper case fields are exported; lower case fields are only visible inside the package.
- Structs are **values**: assignment and function arguments **copy** every field. A copied
  struct still shares what its slice, map and pointer fields point to.
- Structs are comparable with `==` when all their fields are, so they can be map keys.
- Since Go 1.27, a key in a struct literal may also name a **promoted** field of an embedded
  struct (see embedding below).
- Anonymous structs are handy for one-off shapes, often in tests:
  `cases := []struct{ in, want string }{{"a", "A"}}`.

## Pointers

A pointer `*T` holds the address of a `T`. `&x` takes an address, `*p` follows it:

```go
func double(n *int) { *n *= 2 }

x := 21
double(&x)          // x is now 42

p := new(int)       // a pointer to a new zero int
q := new(42)        // Go 1.26: new with an initial value, *q == 42
```

There is no pointer arithmetic, and returning the address of a local variable is safe: the
compiler's **escape analysis** moves it to the heap when it outlives the function. A `nil`
pointer dereference panics, so functions that accept pointers decide and document whether
`nil` is allowed.

`new(expr)` is most useful for optional fields: `Age: new(yearsSince(born))` instead of a
temporary variable or a helper such as `func ptr[T any](v T) *T`.

## Methods

A method is a function with a **receiver**:

```go
func (a Account) Display() string {           // value receiver: works on a copy
	return fmt.Sprintf("%s (%s): %d.%02d", a.ID, a.Owner, a.Balance/100, a.Balance%100)
}

func (a *Account) Deposit(cents int64) {       // pointer receiver: changes the original
	a.Balance += cents
}

acc := Account{ID: "acc-1"}
acc.Deposit(250)          // Go takes &acc for you because acc is addressable
fmt.Println(acc.Display())
```

Choosing the receiver:

| Use a pointer receiver when | Use a value receiver when |
| --- | --- |
| the method changes the receiver | the type is small and immutable in spirit (`time.Time`, a `Point`) |
| the struct is large, to avoid copies | the type is a map, func or channel (already a reference) |
| the struct holds a `sync.Mutex` or other no-copy field | |
| **any other method already has a pointer receiver** (be consistent) | |

You can declare methods on **any named type** defined in the same package, not only structs:

```go
type Celsius float64

func (c Celsius) Fahrenheit() float64 { return float64(c)*9/5 + 32 }

func (c Celsius) String() string { return fmt.Sprintf("%.1f°C", float64(c)) }
```

A `String() string` method is picked up by `fmt` when printing the value (that is the
`fmt.Stringer` interface, which the interfaces lesson explains).

### Constructors and useful zero values

Go has no constructors. When a type needs setup or validation, write a function named
`NewX` that returns it, and an `error` when the input can be invalid:

```go
func NewAccount(id, owner string) (*Account, error) {
	if id == "" {
		return nil, errors.New("account id is required")
	}
	return &Account{ID: id, Owner: owner}, nil
}
```

Better still, design types whose **zero value works** (`bytes.Buffer`, `sync.Mutex`,
`strings.Builder`), so callers can write `var b Builder` and start.

## Embedding

A struct can **embed** another type by naming it without a field name. The embedded type's
fields and methods are **promoted** to the outer type:

```go
type Audit struct {
	CreatedBy string
	CreatedAt time.Time
}

func (a Audit) Age() time.Duration { return time.Since(a.CreatedAt) }

type Invoice struct {
	Audit               // embedded: Invoice gets CreatedBy, CreatedAt and Age()
	Number string
	Total  int64
}

inv := Invoice{Number: "F-1", CreatedBy: "ada"} // Go 1.27: a promoted field as a key
fmt.Println(inv.CreatedBy, inv.Age())           // promoted
fmt.Println(inv.Audit.CreatedBy)                // the explicit path still works
```

Embedding is **composition, not inheritance**: an `Invoice` is not an `Audit`, you cannot
pass it where an `Audit` is expected, and inside `Age` the receiver is the `Audit`, never the
`Invoice`. A field or method declared on the outer type **shadows** the promoted one.

## Struct tags

A string after a field is a **tag**, read by packages through reflection:

```go
type User struct {
	ID    int    `json:"id"`
	Email string `json:"email,omitempty"`
	Pass  string `json:"-"`
}
```

`encoding/json` (and the new `encoding/json/v2` in Go 1.27) uses tags to choose names and
options. Only exported fields are encoded.

## Summary

- Structs group fields; they are values, copied on assignment, and comparable when their
  fields are.
- Pointers share and mutate; there is no pointer arithmetic, and `new(expr)` (1.26) creates
  an initialised pointer.
- Methods attach to any named type. Use pointer receivers to mutate, for large structs and
  for consistency.
- `NewX` functions validate and build; a useful zero value is better when possible.
- Embedding promotes fields and methods: composition, not inheritance.

## Further reading

- A Tour of Go, "Methods and interfaces": <https://go.dev/tour/methods/1>
- Effective Go, "Methods" and "Embedding": <https://go.dev/doc/effective_go#methods>
- The Go spec, "Struct types" and "Method declarations": <https://go.dev/ref/spec#Struct_types>
- Go FAQ, "Should I define methods on values or pointers?": <https://go.dev/doc/faq#methods_on_values_or_pointers>
- Go 1.26 release notes (new with an expression): <https://go.dev/doc/go1.26>
- roadmap.sh, Go topic "Pointer receivers": <https://github.com/nilbuild/developer-roadmap/blob/master/roadmaps/golang/content/pointer-receivers@6NKgb-OjGdT3QsMrDU05b.md>
