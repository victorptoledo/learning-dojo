Grow the grammar and make every mistake a value, never a panic.

- Parentheses group: `(1 + 2) * 3` is `9`, and they nest.
- A `-` in front of a number or a parenthesis negates it: `-4 + 10` is `6`, `2 * -3` is `-6`,
  `-(2 + 3)` is `-5`.

Bad input prints **nothing** on stdout, prints one line `error: <message>` on stderr and exits
with code 1. The message must contain:

| Input | Message contains |
|---|---|
| `""` or only spaces | `empty expression` |
| `1 / 0` (any division by zero) | `division by zero` |
| `2 $ 3` | `unexpected character '$' at position 3` (positions count from 1) |
| `(1 + 2` | `expected ')'` |
| `1 +` | `expected a number` |
| `1 2` | `unexpected '2' at position 3` |

Model the errors as an enum and return them in a `Result`: `main` is the only place that turns
an error into output and an exit code (`std::process::ExitCode`).
