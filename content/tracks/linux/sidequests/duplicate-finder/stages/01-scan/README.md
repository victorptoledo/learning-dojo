`dupes.sh scan DIR` prints the groups of files under `DIR` (in every subfolder) that have
exactly the same content, and exits with `0`.

```text
$ bash dupes.sh scan ~/Downloads
invoice.pdf
old/invoice (1).pdf

photos/cat.jpg
photos/import-2024/IMG_0042.jpg
```

- Each group lists its paths, relative to `DIR` and sorted; groups are separated by one
  empty line and sorted by their first path. No duplicates at all prints nothing.
- Only regular files count: symbolic links are neither followed nor listed. Empty files are
  never duplicates.
- **Read as little as possible.** Two files can only be equal if they have the same size, so
  a file whose size no other file has is never opened. Only files that share a size are
  hashed (`sha256sum`); files with the same size and the same hash are a group.

Errors, Unix style: no command, an unknown command or a wrong number of arguments prints
`usage: dupes.sh scan|report|link [options] DIR` on stderr and exits `2`; a `DIR` that is
not a directory prints `dupes.sh: not a directory: DIR` on stderr and exits `2`.
