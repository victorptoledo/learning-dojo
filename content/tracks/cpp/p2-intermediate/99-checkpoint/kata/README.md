# Config and Transactions

Write the members of `Config` and `Transaction`: `get` and `get_or` in `config.hpp` (they are
templates), the rest in `config.cpp`.

## `Config`

```text
# service settings          <- comments and blank lines are skipped
name = billing
port=8080                   <- spaces around the key and the value are optional
url = http://x.test/?a=b    <- the value is everything after the first '='
empty =                     <- an empty value is fine
```

| Member | Does |
| --- | --- |
| `Config::parse(text)` | A `Config`, or the **first** error: `Syntax` (line without `=`, or a key that is empty or uses anything but `a-z`, `0-9`, `_`, `.`) or `DuplicateKey` (with the key). Errors carry the 1-based line. Lines may end with `"\r\n"`. |
| `get<T>(key)` | The value as `int`, `double` (the whole text must be the number, as `std::from_chars` reads it), `bool` (exactly `true` or `false`) or `std::string`. `MissingKey` or `WrongType` errors carry the key and line 0. |
| `get_or(key, fallback)` | The value, or `fallback` on any error. |
| `raw(key)`, `set`, `erase` | The text of a value (or nothing), insert or replace, remove. |

## `Transaction`

A `Transaction` changes a `Config` at once (`set`, `erase`) and remembers how to undo each
change. `commit()` keeps the changes. `rollback()` undoes them, the last one first. The
destructor rolls back if neither was called, so an early `return` or an exception can never
leave half a change behind. After `commit` or `rollback`, every call throws
`std::logic_error`. It cannot be copied; moving hands the changes over, and the moved-from
transaction does nothing in its destructor.

```cpp
{
    dojo::Transaction change(config);
    change.set("port", "9090");
    if (!valid(config)) {
        return;  // the destructor puts port back to 8080
    }
    change.commit();
}
```

The dojo runs 15 hidden tests and the visible one under AddressSanitizer, plus the tool
checks on both files.
