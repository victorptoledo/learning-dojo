Some files should never travel: build output, `node_modules`, logs, local secrets. Add
excludes in the style of `.gitignore`, and a slower but stricter comparison.

```ts
/** True when the relative path (a folder when isDir) is excluded. */
export type Ignore = (relativePath: string, isDir: boolean) => boolean;
export function compileIgnore(patterns: readonly string[]): Ignore;
export const IGNORE_FILE = ".mirrorignore";
// scan(root, ignore?) skips what ignore excludes, and PlanOptions gains:
//   exclude?: readonly string[]; checksum?: boolean;
```

## Patterns

- Lines are trimmed; blank lines and lines starting with `#` are skipped.
- `*` matches anything but `/`, `?` one character but `/`, `**` anything including `/`
  (`docs/**/draft.md` also matches `docs/draft.md`). Every other character is literal.
- A pattern **without** a `/` matches the **name** at any depth: `*.log`, `node_modules`.
- A pattern **with** a `/` matches the **whole path** from the root: `build/*.map`; a leading
  `/` anchors a simple name to the root: `/dist` matches `dist` but not `web/dist`.
- A trailing `/` matches folders only: `tmp/`.
- A leading `!` brings back a path an earlier pattern excluded. **The last pattern that
  matches decides.**

## Mirroring with excludes

- `scan(root, ignore)` does not list excluded files and **does not enter** excluded folders
  (it never reads them, so an unreadable excluded folder is no error).
- `plan` reads the patterns of `.mirrorignore` at the source root (if there is one), then
  `options.exclude` after them, so a command-line pattern wins. It applies them to **both**
  folders: excluded files are never copied, and excluded files in the target are never
  deleted, even with `delete`. The `.mirrorignore` file itself is mirrored like any file.
- `--exclude PATTERN` may be given several times (`parseArgs` with `multiple: true`).

## --checksum

With `checksum: true` (`--checksum`), two files of the same size are always compared by
SHA-256, even when their times match: a file changed without its time changing becomes an
`update`. Same content with another time is still a `touch`.

`USAGE` becomes
`"usage: mirror [--dry-run] [--delete] [--checksum] [--exclude PATTERN]... <source> <target>"`.
