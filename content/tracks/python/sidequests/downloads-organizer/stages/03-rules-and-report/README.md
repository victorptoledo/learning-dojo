Let people choose, and tell them what they have.

- If `folder` holds a file `.organizer.toml` (hidden, so it is never moved), its categories
  replace the built-in ones:

```toml
[categories]
photos = ["jpg", "png", "heic"]
books = ["pdf", "epub"]
```

  Extensions are written without the dot and compare ignoring case; a file that fits no
  category still goes to `other`. Read it with `tomllib` from the standard library. A broken
  file is `error: bad rules: ...` on stderr with code 1, and nothing moves.
- `main(["report", folder])` moves nothing. It prints one line per category folder that
  holds files (directly inside it), sorted by name, then a total:

```text
documents: 2 files, 1.5 KiB
images: 1 file, 300 B
total: 3 files, 1.8 KiB
```

  Sizes below 1024 bytes are whole bytes (`300 B`); from there on they use KiB, MiB or GiB
  with one decimal (1 KiB = 1024 B). Note `1 file` but `2 files`. Files still at the top of
  the folder are left out; with no category folder at all it prints `total: 0 files, 0 B`.
