Why is `git diff` between two commits instant even in a repository with a million files?
Because a tree's id is the hash of everything below it: when two folders have the same id,
nothing inside them changed, and Git does not even open them.

**`minigit.sh diff REPO A B`** prints the files that differ between the commits `A` and `B`,
as `git diff --name-status --no-renames A B` does: one line `STATUS<TAB>PATH` per file,
sorted by path in byte order.

```text
$ bash minigit.sh diff ~/src/shop v1.0 main
M	README.md
D	src/old.txt
A	src/search/index.txt
T	start.sh
```

- `A` the file is only in `B`, `D` only in `A`, `M` it is in both with other content or
  other mode (`100644` and `100755`), `T` its type changed (a file became a symbolic link,
  or the other way round). A submodule entry is compared by its commit id, like a file.
- A file replaced by a folder of the same name is a `D` for the file and an `A` for each
  file of the folder (and the other way round).
- **Walk both trees side by side and skip every entry whose id is the same on both sides.**
  The tests record what your script asks `cat-file` for and fail if it opens a tree that is
  identical in `A` and `B`. A folder that exists on one side only must be opened, to list
  its files.
- The same commit twice, or two commits with the same tree, print nothing.

Plumbing allowed: `cat-file` and `rev-parse`. `A` and `B` are revisions as in `log`; an
unknown one prints `minigit.sh: unknown revision REV` and exits `1`. A wrong number of
arguments or a `REPO` that is not a Git repository is the usage error (`2`).
