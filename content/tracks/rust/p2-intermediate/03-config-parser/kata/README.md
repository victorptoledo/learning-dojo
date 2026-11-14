# Config Parser

Implement `parse_config(text)` in `src/lib.rs`. It reads lines of `key = value` into a
`BTreeMap<String, String>`.

- Blank lines and lines starting with `#` (after leading spaces) are ignored.
- Keys and values are trimmed. A value may be empty; a value may contain `=`
  (only the first `=` separates key and value).
- A line without `=` is `ConfigError::MissingEquals { line }`.
- An empty key is `ConfigError::EmptyKey { line }`.
- A key seen twice is `ConfigError::DuplicateKey { key, line }`, where `line` is the second one.
- Line numbers start at 1 and count every line, including ignored ones.

Fill in the `Display` implementation of `ConfigError` with these messages (`std::error::Error` is already implemented):

| Variant | Message |
| :--- | :--- |
| `MissingEquals { line: 3 }` | `line 3: expected key = value` |
| `EmptyKey { line: 2 }` | `line 2: empty key` |
| `DuplicateKey { key: "port", line: 4 }` | `line 4: duplicate key "port"` |

No `unwrap` or `expect` in the solution: return errors with `?` or `Err(...)`.
