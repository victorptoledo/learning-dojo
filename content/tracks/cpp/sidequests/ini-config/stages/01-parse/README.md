Parse INI text. `config.hpp` declares the interface; add the data members and write the
members in `config.cpp`.

The text is read line by line (`\n` or `\r\n`; the last line may have no line break). Each
line, with its spaces and tabs trimmed, is one of:

| Line | Means |
| --- | --- |
| empty, or starting with `#` or `;` | nothing (a comment) |
| `[name]` | opens the section `name` (spaces inside the brackets are trimmed); keys that follow belong to it |
| `key = value` | a key of the open section. Only the first `=` splits; key and value are trimmed, and the value may be empty or hold more `=` |

- Keys before any `[section]` belong to the section `""`.
- Section and key names are one or more of `A-Z a-z 0-9 _ - .`, and are case-sensitive.
- Opening a section again adds keys to it; it keeps its first place.

`Config::parse(text)` returns the `Config`, or a `ParseError` with the line (counting from 1)
of the **first** bad line and its kind:

| Kind | When |
| --- | --- |
| `BadSection` | a line starting with `[` that does not end with `]`, or a bad name (`[]`, `[a b]`) |
| `BadKey` | a `key = value` line whose key is empty or not a name (`my key = 1`) |
| `MissingEquals` | any other line (`just words`) |
| `DuplicateKey` | a key the section already has |

`raw(section, key)` is the value as text, `std::nullopt` when the section or key does not
exist. `sections()` lists the sections in the order they first appear, `""` first and only
when it has keys; `keys(section)` lists its keys in order (none for an unknown section).

Order matters, so a `std::map` is not enough on its own: think about what keeps the order you
read things in. `std::ranges::find` with a projection (`&Entry::first`) finds a key in a
vector of pairs.
