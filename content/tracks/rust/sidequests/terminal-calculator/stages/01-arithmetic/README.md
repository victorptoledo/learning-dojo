`calc "<expression>"` prints the value of the expression on one line and exits with code 0.

- Numbers are decimal: `7`, `2.5`, `0.25`. Compute with `f64`.
- The operators are `+`, `-`, `*` and `/`. `*` and `/` bind tighter than `+` and `-`, and
  operators of the same strength go from left to right: `7 - 2 - 1` is `4`, `8 / 2 / 2` is `2`.
- Spaces are optional: `2*3+4` and `2 * 3 + 4` are the same.
- Print the result with `f64`'s `Display` (`println!("{value}")`): `7`, `2.5`,
  `0.3333333333333333`.

| Command | Prints |
|---|---|
| `calc "1 + 2"` | `3` |
| `calc "2 + 3 * 4"` | `14` |
| `calc "10 / 4"` | `2.5` |
| `calc "7 - 2 - 1"` | `4` |

Only well-formed input is checked in this stage.
