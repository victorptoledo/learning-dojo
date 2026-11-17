Polish for daily use:

- Hidden files and folders (names starting with `.`, like `.git`) are skipped by `search` and
  `index`.
- `search` takes options **after** `<dir> <text>`:
  - `--ignore-case`: `Todo`, `TODO` and `todo` all match `todo`.
  - `--ext rs,md`: only files whose extension is in the list.
  Both can be given, in any order. An unknown option is a usage error (exit 2).
- `query` takes several words: `findex query <index-file> <word>...` lists the files that
  contain **every** word, with `count` the sum of their counts, in the same order as before.
