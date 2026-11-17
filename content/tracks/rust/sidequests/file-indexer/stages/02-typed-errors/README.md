Real folders hold binaries and typos happen. Give every failure a type and an exit code:

| Situation | Exit code | stderr |
|---|---|---|
| a match was found | 0 | — |
| nothing matched | 1 | — |
| wrong or missing arguments | 2 | a line starting with `usage: findex` |
| `<dir>` does not exist or is not a folder | 2 | `error: cannot read <dir>: ...` |

- A file that is not valid UTF-8 is **skipped**, with `warning: skipping <path>: not UTF-8`
  on stderr (`<path>` relative, like in the results), and the search goes on. Warnings do not
  change the exit code.
- An unknown command (`findex frobnicate`) is a usage error too.

A good shape: an `enum FindexError` with one variant per case, `impl Display` for the
messages and one function that maps a variant to its exit code. `main` returns
`std::process::ExitCode`.
