Look before you copy: list both folders and work out what has to change. Export from
`mirror.ts` (the starter already declares the types):

| Export | Does |
| --- | --- |
| `scan(root)` | Every **regular file** under `root` as `{ path, size, mtimeMs }`: `path` relative to `root` with `/` separators, `mtimeMs` in whole milliseconds (`Math.trunc` of `stat`'s `mtimeMs`). Sorted by `path` (plain string order). Folders are entered, symbolic links are skipped (to files and to folders). Rejects with the system error (`code: "ENOENT"`, ...) when `root` cannot be read |
| `plan(source, target, options?)` | The `Action[]` that makes `target` match `source` (below) |
| `formatPlan(actions)` | The plan as lines of text (below) |
| `main(argv, io)` | The program; this stage only plans (below) |
| `USAGE` | `"usage: mirror [--dry-run] [--delete] <source> <target>"` |

## plan

For each file of the source, in path order:

| The target's file | Action |
| --- | --- |
| does not exist | `{ op: "copy", path, size }` |
| has another size | `{ op: "update", path, size }` |
| has the same size and the same `mtimeMs` | nothing: it is taken as unchanged |
| has the same size but another time | compare the SHA-256 of both: the same content is a `touch` (only the time needs fixing), different content is an `update` |

`size` is always the source file's size. Hashing reads both files, so only hash in that last
case: the tests make files unreadable to prove you never open one when size and time already
decide. A target folder that does not exist yet counts as empty.

With `options.delete`, every file the target has and the source does not becomes
`{ op: "delete", path, size }` (the target file's size). Deletes come **after** all other
actions, in path order: copy first, delete last, so an interrupted run never loses data.

## formatPlan

One line per action, `<op> <path>`, then a summary line:
`copy <n>, update <n>, touch <n>, delete <n>, <bytes> bytes to transfer`, where the bytes
are the sizes of the copies and updates. An empty plan is the single line `nothing to do`.

## main

`mirror [--dry-run] [--delete] <source> <target>`, parsed with `parseArgs` in strict mode.
This stage only plans: print `formatPlan` on stdout and return `0` (the next stage makes
`--dry-run` mean something).

| Situation | stderr | Exit code |
| --- | --- | --- |
| Unknown option, or not exactly two folders | `error: <message>`, then `USAGE` | `2` |
| The source cannot be read | `error: cannot read <source>: <reason>`, the reason being `no such folder` (`ENOENT`), `not a folder` (`ENOTDIR`, or a file), `permission denied` (`EACCES`) or the error's message | `2` |
| The target exists but is not a readable folder | `error: cannot read <target>: <reason>`, the same way (a missing target is fine) | `2` |
| The two folders are the same, or one is inside the other (compare `path.resolve`d paths) | `error: source and target overlap` | `2` |

Try it with `node bin.ts --dry-run <source> <target>`, and run your tests with `npm test`
after `npm install`.
