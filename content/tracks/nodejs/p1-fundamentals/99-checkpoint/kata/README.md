# Checkpoint 1: dupes

Write `dupes`, a command-line tool that finds files with the same content under a folder. It
brings the whole phase together: async iteration over the file system, paths, hashing,
`parseArgs`, error codes from the operating system and exit codes.

```text
$ node bin.ts photos
2 files, 2048 bytes each:
  2024/beach.jpg
  backup/beach copy.jpg
3 files, 11 bytes each:
  a.txt
  b.txt
  docs/readme.md
3 duplicate files waste 2070 bytes
$ echo $?
1
```

Write five functions in `dupes.ts`. `bin.ts` is done.

| Function | Does |
| --- | --- |
| `walk(root, options?)` | An **async generator** that yields every regular file under `root`, as a path relative to `root` with `/` separators (also on Windows). A folder's entries are visited in sorted name order (plain `sort()` order), sub-folders depth first. Symbolic links are skipped, and so are folders whose **name** is in `options.ignore` (default `["node_modules", ".git"]`). It rejects with the system error (for example `code: "ENOENT"`) when `root` cannot be read |
| `hashFile(file)` | The SHA-256 of the file's content, as 64 hex characters |
| `findDuplicates(root, options?)` | The groups of files with the same content (see below) |
| `formatReport(groups)` | The text report as lines (see below) |
| `main(argv, io)` | The program (see below) |

## findDuplicates

- Skips files smaller than `options.minSize` bytes (default `1`, so empty files are skipped).
- Hashing is the slow part, so first group the files by size (`stat`) and **only hash files
  whose size is shared** by another file. Files of the same size with different content are
  not duplicates.
- Each group is `{ size, hash, files }` with `files` sorted. Groups are sorted by size, largest
  first, then by their first file.

## formatReport

For each group, `<n> files, <size> bytes each:` and then each file indented by two spaces. A
last line sums it up: `<extra> duplicate files waste <bytes> bytes`, where a group of `n` files
counts `n - 1` extra files and `size * (n - 1)` wasted bytes. With no groups the report is the
single line `no duplicates`.

## main

`dupes [--json] [--min-size BYTES] <folder>`, parsed with `parseArgs` in strict mode.

| Situation | Output | Exit code |
| --- | --- | --- |
| No duplicates | the report on stdout | `0` |
| Duplicates found | the report on stdout | `1` |
| With `--json` | instead of the report, a single `io.stdout(JSON.stringify(groups, null, 2))` | `0` or `1` as above |
| Unknown option, not exactly one folder, `--min-size` not a whole number of at least 0 | `error: <message>` then `USAGE` on stderr | `2` |
| The folder cannot be read | `error: cannot read <folder>: <reason>` on stderr, where the reason is `no such folder` (`ENOENT`), `not a folder` (`ENOTDIR`), `permission denied` (`EACCES`) or the error itself | `2` |

Exit code 1 for "found something" is what tools like `grep` and linters do, so a script or a
CI job can test the result.

Run the tests with `npm test` after `npm install`, or try the tool with `node bin.ts .`. There
are 17 tests: 1 in `tests/visible.test.ts` and 16 hidden ones.
