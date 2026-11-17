`units convert <value> <from> <to>` prints one line and exits with 0:

```text
$ units convert 10 km m
10 km = 10000 m
$ units convert 1 mi ft
1 mi = 5280 ft
```

| Unit | In metres |
|---|---|
| `mm` | 0.001 |
| `cm` | 0.01 |
| `m` | 1 |
| `km` | 1000 |
| `in` | 0.0254 |
| `ft` | 0.3048 |
| `yd` | 0.9144 |
| `mi` | 1609.344 |

Numbers (the value as read, and the result) are printed rounded to 4 decimals, without
trailing zeros: `2.5`, `0.3048`, `10000`, `0.0001`. `strconv.FormatFloat(x, 'f', -1, 64)`
after rounding with `math.Round(x*10000)/10000` does it.

Only good input is used in this stage.
