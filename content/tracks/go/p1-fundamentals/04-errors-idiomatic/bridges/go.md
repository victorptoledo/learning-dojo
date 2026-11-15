---
from: go
status: draft
concept: "Error handling since Go 1.13"
from_way: "github.com/pkg/errors for Wrap and Cause, comparing err == ErrX, type assertions on the outer error, multierror libraries, and a var target declared before every errors.As."
native_way: "fmt.Errorf with %w, errors.Is and errors.As (1.13), errors.Join and several %w in one Errorf (1.20), errors.AsType (1.26), and go vet checks for %w misuse (1.27)."
sensei_tip: "Search the code base for `== Err` and `.(*` on errors: each hit stops working the day someone adds context with %w. Replace them with errors.Is and errors.AsType."
---

| Older Go | Go 1.27 |
| --- | --- |
| `errors.Wrap(err, "load")` (pkg/errors) | `fmt.Errorf("load: %w", err)` |
| `errors.Cause(err) == ErrX` | `errors.Is(err, ErrX)` |
| `if e, ok := err.(*MyErr); ok` | `if e, ok := errors.AsType[*MyErr](err); ok` |
| `var e *MyErr; if errors.As(err, &e)` | `errors.AsType` (1.26) |
| hashicorp/go-multierror | `errors.Join(a, b)` (1.20) |
| `interface{}` in Must helpers | generics: `func Must[T any](v T, err error) T` |
