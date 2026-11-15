# Sales Report

Write `sales.sh [-m YYYY-MM] [-r REGION] FILE`. `FILE` is a CSV export with a header line:

```text
date,region,product,quantity,unit_price
2026-10-03,North,keyboard,2,25.00
...
```

It prints one line per region, **sorted by region** (byte order), then a `TOTAL` line:

```text
$ bash sales.sh sample.csv
region,orders,units,revenue
East,1,1,25.00
North,3,6,228.50
South,2,6,318.50
TOTAL,6,13,572.00
```

`orders` counts records, `units` adds the quantities and `revenue` adds
`quantity * unit_price`, printed with two decimals.

## Options

- `-m 2026-11` keeps only records whose date is in that month. A month that is not
  `YYYY-MM` (with a month from 01 to 12) prints `sales.sh: invalid month: VALUE` on stderr
  and exits `2`.
- `-r North` keeps only that region (exact match).
- Both can be combined. With no matching records the report is the header and
  `TOTAL,0,0,0.00`.

## Input rules

- The first line is the header and is skipped. Blank lines are skipped.
- Lines may end with Windows line ends (`\r\n`).
- A record is **invalid** when it does not have exactly 5 fields, the quantity is not a
  whole number of at least 1, or the unit price is not a number with at most two decimals
  (`9`, `9.5` and `9.50` are fine). Each invalid record prints
  `sales.sh: line N: invalid record` on **stderr** (N is the line number in the file), is
  skipped, and the report goes on. The exit status stays `0`.

## Errors

- an unknown option, or not exactly one `FILE`: `usage: sales.sh [-m YYYY-MM] [-r REGION] FILE`
  on stderr, exit `2`;
- a file that cannot be read: `sales.sh: cannot read FILE` on stderr, exit `1`.

Tools: `getopts ':m:r:'`, `shift $((OPTIND - 1))`, `[[ $m =~ ... ]]`, `awk -F, -v`,
`next`, associative arrays, `printf "%.2f"`, `print ... | "sort"` and `close()`.

There are 11 tests: 1 in `tests/visible.bats` and 10 hidden ones, plus ShellCheck.
