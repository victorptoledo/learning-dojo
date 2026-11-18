Now carry the plan out. Without `--dry-run`, `main` plans and then applies the plan; with it,
it prints the plan as before and changes nothing. Export the function that does the work:

```ts
export interface ApplyResult { done: number; failed: number }
export function apply(source: string, target: string, actions: readonly Action[], io: Io): Promise<ApplyResult>;
```

**Each action**, in the plan's order, creating `target` and any missing folders on the way:

- `copy` and `update`: stream the source file into a **temporary file in the same folder**
  (for example `.<name>.mirror-tmp`) with `pipeline`, give it the source's permission bits
  (`mode & 0o777`) and times (`utimes`), and only then `rename` it over the final name. A
  reader of the target sees the old file or the new one, never half of one: the tests check
  that an updated file is a new inode. Streams keep memory flat for files of any size.
- `touch`: set the target file's times to the source's.
- `delete`: remove the file. Once all deletes are done, remove the folders they left
  **empty**, deepest first, unless the source has that folder too.

**Output:** after each action that worked, `copied <path>`, `updated <path>`, `touched <path>`
or `deleted <path>` on stdout. A failed action prints
`error: <op> <path>: <reason>` on stderr (`permission denied`, `no such file or folder`,
`not a folder`, or the error's message) and **does not stop the others**. If it failed half
way, no temporary file is left and the old target file is still whole.

**main** prints the action lines, then `done: <ok> ok, <failed> failed`, and returns `0`, or
`1` when an action failed. When the plan is empty it prints only `nothing to do` and returns
`0`. Because times and permissions are copied, running `mirror` twice in a row finds nothing
to do the second time.
