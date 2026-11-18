`scrub.sh purge REPO PATH...` removes each `PATH` from **every commit of every branch and
tag**, as if it had never been committed. A `PATH` is a file or a folder (everything under
it), written from the top of the repository: `media/demo.mp4`, `config`. It matches whole
names only: purging `config` keeps `config.bak` and `configs/`.

```text
$ bash scrub.sh purge ~/src/shop media/demo.mp4 config/prod.env
rewrote 182 commits, dropped 1
```

What a good rewrite keeps, and the tests check:

- **Everything else is identical:** each commit keeps its author, committer, dates and
  message, and its tree loses only the purged paths. Commits older than the first one that
  touched a purged path are not rewritten at all: **they keep their ids**.
- **Empty commits are dropped.** A commit whose changes were all in purged paths has nothing
  left to say: it is dropped and its children hang from its parent (a root commit that ends
  up empty is dropped and its child becomes the root). Merge commits are kept, and so are
  commits that were empty on purpose before the rewrite.
- **Branches and tags follow:** every branch points at the rewritten version of its commit
  (or at the parent of a dropped one). Annotated tags are rewritten with the same name,
  message and tagger; lightweight tags move too.
- The branch checked out stays checked out, and **the index and working tree match its new
  commit**: the purged files are gone from disk too, like with `git filter-repo`. Untracked
  files are left alone.
- It prints `rewrote N commits, dropped M`: `N` commits got a new id, `M` were dropped.

**How.** `git fast-export --branches --tags --no-data` writes history as a text stream: one
block per commit with its header, `from` and `merge` lines naming the parents (`:12` is the
mark of an earlier commit) and one `M mode blob path` or `D path` line per changed file.
Remove the lines of purged paths, drop the commits left without any, and feed the result to
`git fast-import --force`. Two traps: a `data N` line is followed by exactly `N` bytes of
message (which may contain empty lines, or no final newline), and a dropped commit's mark may
still be named by later `from`, `merge`, `reset` and `tag` blocks. `--export-marks` on both
sides tells you which old commit became which new one.

Refuse, with nothing changed: uncommitted changes to tracked files
(`scrub.sh: uncommitted changes`) and a repository without commits (`scrub.sh: no commits`),
both exiting `1`. No `PATH` at all is the usage error (`2`).
