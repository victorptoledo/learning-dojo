# Errors as Values: Wrapping, errors.Is and errors.As

Go has no exceptions for ordinary failures. A function that can fail **returns an error as
its last result**, and the caller decides what to do with it. The famous
`if err != nil` is not ceremony: it puts every failure path in plain sight.

## The error interface

```go
type error interface {
	Error() string
}
```

Any type with an `Error() string` method is an error. A `nil` error means success:

```go
f, err := os.Open("config.yaml")
if err != nil {
	return fmt.Errorf("load config: %w", err)
}
defer f.Close()
```

The convention: check the error **immediately**, handle it or return it, and keep the
success path unindented (no `else` after a `return`).

## Creating errors

```go
var ErrNotFound = errors.New("not found")              // a sentinel: a fixed, comparable value

func Find(id string) (User, error) {
	u, ok := users[id]
	if !ok {
		return User{}, fmt.Errorf("find user %q: %w", id, ErrNotFound)
	}
	return u, nil
}
```

- `errors.New` makes a simple error; `fmt.Errorf` formats one.
- Error strings start in **lower case** and have no final punctuation, because they get
  joined: `load config: open config.yaml: no such file or directory`.
- Exported sentinel errors are named `ErrXxx` (`io.EOF`, `fs.ErrNotExist`, `sql.ErrNoRows`).

### Custom error types

When callers need **data** about the failure, define a type:

```go
type ValidationError struct {
	Field  string
	Reason string
}

func (e *ValidationError) Error() string {
	return fmt.Sprintf("invalid %s: %s", e.Field, e.Reason)
}
```

## Wrapping, errors.Is and errors.As

`%w` in `fmt.Errorf` **wraps** an error: the new error carries a message and keeps the
original inside, reachable with `errors.Unwrap`. This builds a chain:

```text
"create order: validate: invalid email: missing @"
   └── wraps "validate: invalid email: missing @"
          └── wraps *ValidationError{Field: "email", ...}
```

Never compare a possibly wrapped error with `==`. Use the functions that walk the chain:

```go
if errors.Is(err, ErrNotFound) {           // is any error in the chain ErrNotFound?
	http.Error(w, "no such user", http.StatusNotFound)
}

var ve *ValidationError
if errors.As(err, &ve) {                   // is any error in the chain a *ValidationError?
	log.Printf("bad field %s", ve.Field)
}

if ve, ok := errors.AsType[*ValidationError](err); ok {   // Go 1.26: generic, no variable first
	log.Printf("bad field %s", ve.Field)
}
```

- `%v` instead of `%w` formats the message but **hides** the cause: use it when the
  underlying error is an implementation detail you do not want callers to depend on.
- `errors.Join(err1, err2)` and `fmt.Errorf("...: %w, %w", a, b)` combine several errors;
  `Is` and `As` look inside all of them.
- Since Go 1.27, `go vet` warns when you wrap a pointer to an error value with `%w` in a way
  that breaks `errors.Is` and type assertions.

## Handle an error once

An error is either **handled** (logged, retried, turned into a response, replaced by a
default) or **returned with context**. Doing both (logging and returning) prints the same
failure several times, once per layer.

```go
// Low level: add context and return
return fmt.Errorf("charge card %s: %w", cardID, err)

// Boundary (HTTP handler, main, a job runner): handle it
if err != nil {
	slog.Error("payment failed", "order", orderID, "err", err)
	http.Error(w, "payment failed", http.StatusBadGateway)
	return
}
```

Good context says **what was being done** ("charge card 42"), not "error occurred".

## defer

`defer` schedules a call to run when the function returns, in **last in, first out** order.
It is how Go releases resources:

```go
func copyFile(dst, src string) (err error) {
	in, err := os.Open(src)
	if err != nil {
		return err
	}
	defer in.Close()

	out, err := os.Create(dst)
	if err != nil {
		return err
	}
	defer func() {
		if cerr := out.Close(); err == nil {
			err = cerr        // a failed Close on a written file is a real error
		}
	}()

	_, err = io.Copy(out, in)
	return err
}
```

The arguments of a deferred call are evaluated when `defer` runs, not when the call happens.
A deferred closure can change **named results**, as above.

## panic and recover

`panic` stops the normal flow and unwinds the stack, running deferred calls; an unrecovered
panic crashes the program with a stack trace. It is for **programmer errors and impossible
states** (an index out of range, a nil map write, a broken invariant), not for expected
failures such as a missing file or bad input.

`recover`, called in a deferred function, stops a panic. Servers use it at the boundary so
one bad request does not kill the process (`net/http` already does this per request). Do
not use panic and recover as exceptions inside your API.

`Must` helpers (`regexp.MustCompile`, `template.Must`) panic on error and are meant for
values known at program start, such as a constant pattern.

## Summary

- Errors are values: return `error` last, check it at once, keep the happy path flat.
- Sentinels (`ErrXxx`) for conditions, custom types for data, lower case messages.
- Wrap with `%w` to add context; inspect with `errors.Is`, `errors.As` or `errors.AsType`.
- Handle an error once: return it with context, or log and handle it at a boundary.
- `defer` for cleanup, LIFO; `panic` only for bugs and impossible states.

## Further reading

- Working with Errors in Go 1.13 (Go blog): <https://go.dev/blog/go1.13-errors>
- Error handling and Go (Go blog): <https://go.dev/blog/error-handling-and-go>
- Package errors (Is, As, AsType, Join): <https://pkg.go.dev/errors>
- Effective Go, "Errors" and "Panic": <https://go.dev/doc/effective_go#errors>
- Defer, Panic, and Recover (Go blog): <https://go.dev/blog/defer-panic-and-recover>
- roadmap.sh, Go topic "Wrapping/Unwrapping errors": <https://github.com/nilbuild/developer-roadmap/blob/master/roadmaps/golang/content/wrappingunwrapping-errors@s2WlOMMKNXf6O2Qiqcm_m.md>
