`findex search <dir> <text>` walks `<dir>` and every folder below it and prints each line
that contains `<text>` (case-sensitive), as `path:line: content`:

```text
$ findex search notes todo
a.txt:3: todo: buy milk
sub/plan.md:1: todo list
```

- `path` is relative to `<dir>`, with `/` between folders; `line` counts from 1; `content` is
  the line as it is in the file (without its line ending).
- Results are sorted by path, then by line number.
- Exit code 0 when something matched, 1 when nothing did (like `grep`).

Every file in this stage is valid UTF-8 text.
