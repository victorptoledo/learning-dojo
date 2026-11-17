A second category, and errors that tell the user what went wrong.

Mass units, in grams: `mg` 0.001, `g` 1, `kg` 1000, `oz` 28.349523125, `lb` 453.59237.

| Situation | Exit code | stderr |
|---|---|---|
| no arguments, an unknown command, the wrong number of arguments | 2 | a line starting with `usage: units` |
| a unit nobody knows | 1 | `error: unknown unit "parsec"` |
| a value that is not a number | 1 | `error: not a number: "ten"` |
| units of two categories | 1 | `error: cannot convert length to mass` (the categories of `<from>` and `<to>`) |

Nothing goes to stdout when the command fails. Units compare exactly (`KM` is unknown).

In Go, errors are values: return them up to `Run`, and let `Run` alone choose the message
and the exit code (`errors.Is` / `errors.As` with your own error types work well here).
