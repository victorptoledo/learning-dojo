Reading history is walking the object graph. Plumbing allowed in this stage: `cat-file` and
`rev-parse` (to turn a name like `main`, `v1.0` or `HEAD~2` into an id).

A commit object (`git cat-file -p ID`, or `git cat-file commit ID` for the raw form) is a few
header lines, an empty line and the message:

```text
tree 9c1e...
parent 4b02...
parent 77fa...
author Ana Souza <ana@shop.example> 1719835200 +0000
committer Ana Souza <ana@shop.example> 1719835200 +0000

Merge branch 'search'
```

**`minigit.sh log REPO [REV]`** prints the history of `REV` (default `HEAD`) along **first
parents**, newest first, one line per commit: its id and its subject. It must print what
`git log --first-parent --format='%H %s' REV` prints:

- the subject is the message's first paragraph (up to the first empty line), its lines
  joined with one space;
- on a merge, follow only the first `parent` line; the root commit has none.

**`minigit.sh ls-tree REPO REV`** lists every file of the commit's tree, recursively, as
`git ls-tree -r REV` does: `MODE TYPE ID<TAB>PATH`, in tree order (each folder's entries in
the order stored, a subfolder's contents where the subfolder is). Folders themselves are not
listed. A submodule is an entry of type `commit` (mode `160000`): list it, but there is no
tree to walk into.

For both, `REV` may be a branch, a tag (annotated tags point at a commit: `REV^{commit}`), a
commit id or an expression `rev-parse` understands. A `REV` that does not name a commit (or
`HEAD` in a repository without commits) prints `minigit.sh: unknown revision REV` on stderr
and exits `1`. A wrong number of arguments or a `REPO` that is not a Git repository is the
usage error (`2`).
