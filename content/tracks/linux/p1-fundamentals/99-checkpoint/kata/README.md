# Checkpoint 1: Inventory

Write `inventory.sh DIR`. It prints an inventory of everything under `DIR`:

```text
$ bash inventory.sh project
files: 6
directories: 3
bytes: 1500
extensions:
2 txt
1 (none)
1 gz
1 jpg
1 sh
largest:
1000 data/archive.tar.gz
300 docs/notes.txt
100 Makefile
world-writable: 2
```

| Line | Meaning |
| --- | --- |
| `files` | regular files anywhere under `DIR` |
| `directories` | directories under `DIR`, **not** counting `DIR` itself |
| `bytes` | the sum of the sizes of the regular files |
| `extensions` | one line `COUNT EXT` per extension, most frequent first, equal counts in alphabetical order |
| `largest` | the 3 biggest files as `SIZE PATH` (path relative to `DIR`), biggest first, equal sizes by path; fewer lines if there are fewer files |
| `world-writable` | files and directories under `DIR` that others may write |

The **extension** is what follows the last dot of the file name, in lower case:
`photo.JPG` is `jpg` and `archive.tar.gz` is `gz`. A name without a dot (`Makefile`), a dot
only at the start (`.bashrc`) or a dot at the very end (`draft.`) has no extension and
counts as `(none)`.

An empty directory prints `files: 0`, `directories: 0`, `bytes: 0`, `extensions:`,
`largest:` and `world-writable: 0`.

Errors, as in the permission audit: no argument or more than one prints
`usage: inventory.sh DIR` on stderr and exits `2`; something that is not a directory prints
`inventory.sh: not a directory: DIR` on stderr and exits `2`.

There are 9 tests: 1 in `tests/visible.bats` and 8 hidden ones, plus ShellCheck.
