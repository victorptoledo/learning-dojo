Temperatures, a flag, and a way to discover the units.

- Temperature units `C`, `F` and `K`. They are not plain factors: `°F = °C × 9/5 + 32` and
  `K = °C + 273.15`. A temperature below absolute zero (0 K) is exit 1 with
  `error: below absolute zero`.
- `--precision <n>` (0 to 10, default 4), given right after `convert`, sets the decimals of the
  printed numbers: `units convert --precision 2 1 in cm` prints `1 in = 2.54 cm`. A bad
  precision is a usage error (exit 2). Use the `flag` package with a `flag.FlagSet` of your
  own (`flag.ContinueOnError`, output sent to `stderr`), never the global one.
- Like every Unix tool built on `flag`, a negative value needs `--` before it:
  `units convert -- -40 C F` prints `-40 C = -40 F`.
- `units list` prints the categories in this order, each with its units sorted:

```text
length: cm ft in km m mi mm yd
mass: g kg lb mg oz
temperature: C F K
```
