---
from: go
status: draft
concept: "Null safety and errors"
from_way: "Multiple return values with error, nil for absence, and if err != nil after every call."
native_way: "Result<T, E> and Option<T>, with ? doing what if err != nil { return err } does."
sensei_tip: "Rust's ? is Go's if err != nil { return err } in one character, plus a conversion of the error type through From. fmt.Errorf with %w becomes map_err or the thiserror crate."
---

| Go | Rust |
| --- | --- |
| val, err := f() | let val = f()?; |
| if err != nil { return err } | ? |
| nil pointer | None |
| errors.Is(err, ErrNotFound) | matches!(err, MyError::NotFound) |
| fmt.Errorf("...: %w", err) | map_err or #[from] with thiserror |
