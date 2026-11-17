Searching re-reads every file. An index reads them once.

- `findex index <dir> <index-file>` reads every UTF-8 file under `<dir>` (skipping the others
  as before) and writes `<index-file>`. It prints `indexed <n> files` and exits 0. The format of
  the index file is yours: lines of text are fine.
- `findex query <index-file> <word>` answers **from the index alone**: the source folder may be
  gone. It prints the files that contain the word, one per line, as `path (count)`, where
  `count` is how many times the word appears in that file. The most frequent first; equal
  counts by path. Exit 0 when a file matched, 1 when none did.

Words are the runs of letters and digits (`char::is_alphanumeric`), compared in lower case:
`Rust`, `rust,` and `RUST!` are all the word `rust`, and the query word is lowered too.

- A missing or unreadable index file is exit code 2 with `error: cannot read <index-file>: ...`.
- `search` keeps working as before.
