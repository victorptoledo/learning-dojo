# Health Report

Write `health.sh [-p PROC] [-d DF_FILE]`. It prints a short health report and exits like a
monitoring probe: `0` when everything is fine, `1` when something needs attention.

```text
$ bash health.sh
memory: 62% used (4960 of 8000 MiB)
load: 1.20 on 4 cpus (0.30 per cpu)
disk /: 81% used
disk /data: 95% used WARN
status: WARN (1 check)
```

## Sources

| Line | From |
| --- | --- |
| memory | `PROC/meminfo`: `MemTotal` and `MemAvailable` (in kB). Used = total minus available. |
| load | `PROC/loadavg`: the first number (the 1-minute load average); the CPU count is the number of lines starting with `processor` in `PROC/cpuinfo`. |
| disks | the output of `df -P`, or the file `DF_FILE` with that output when `-d` is given. |

`PROC` is `/proc` unless `-p` gives another directory (the tests use fake ones).

## Format

- `memory: P% used (U of T MiB)`: `P` is the used percentage rounded **down**, `U` and `T`
  are used and total in MiB (kB divided by 1024, rounded down).
- `load: L on N cpus (X per cpu)`: `L` as in the file, `X` is `L / N` with two decimals.
- one `disk MOUNT: P% used` line per filesystem, in the order of `df`, skipping the header
  and filesystems named `tmpfs`, `devtmpfs` or `udev` (the first column).
- A line gets ` WARN` at the end when memory is at least **90%**, the load per CPU at least
  **1.00**, or a disk at least **90%**.
- The last line is `status: OK`, or `status: WARN (N check)` / `(N checks)` with the number
  of lines marked `WARN`. The exit status is `0` for OK and `1` for WARN.

## Errors

A source file that cannot be read prints `health.sh: cannot read PATH` on stderr and exits
`2`. An unknown option or any operand prints `usage: health.sh [-p PROC] [-d DF_FILE]` on
stderr and exits `2`.

There are 9 tests: 1 in `tests/visible.bats` and 8 hidden ones, plus ShellCheck.
