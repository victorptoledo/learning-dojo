The second backup should take seconds, not hours.

- A copy keeps the original's **permission bits** (a `0755` script stays executable, a
  `0600` key stays private) and its **modification time** (`os.Chtimes`).
- A file whose copy already has the same size and the same modification time is `unchanged`:
  it is not copied and no line is printed for it (this is how `rsync` decides by default).
  Every other file is copied again.
- `--prune` (after `<dst>`) also deletes the files of `<dst>` that are no longer in `<src>`,
  printing `removed <path>` for each, after the `copied` lines, and the summary gains
  `, 1 removed`:

```text
copied notes.txt
removed old/draft.txt
summary: 1 copied, 3 unchanged, 1 removed
```

Errors, Unix style: wrong arguments or an unknown command or option is exit 2 with a line
starting with `usage: backup` on stderr; a `<src>` that is not a folder is exit 1 with
`error: ...` on stderr.
