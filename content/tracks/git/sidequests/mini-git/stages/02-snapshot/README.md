`git commit` needs the index; the objects do not. **`minigit.sh snapshot REPO DIR BRANCH
MESSAGE`** commits the files of the folder `DIR` (with the rules of `hash tree`) as the next
commit of the branch `BRANCH` of the repository `REPO`, and prints the new commit's id. Backup
tools, CI bots and `git stash` itself write commits this way.

Plumbing allowed: `hash-object`, `mktree`, `commit-tree`, `update-ref`, `rev-parse` and
`cat-file`.

1. Write a blob for each file (`git hash-object -w`; a symbolic link's blob is its target).
2. Write the trees from the deepest folder up (`git mktree`, which reads `ls-tree`-style
   lines: `MODE TYPE ID<TAB>NAME`; `-z` for NUL-terminated ones). Its id must be the one
   `hash tree DIR` computes.
3. Write the commit (`git commit-tree TREE -p PARENT -m MESSAGE`): its parent is the current
   tip of `BRANCH`, or none when the branch does not exist yet. Author and committer come
   from Git's usual settings and `GIT_AUTHOR_*` / `GIT_COMMITTER_*` variables.
4. Move `refs/heads/BRANCH` to it **with the old value** (`git update-ref REF NEW OLD`, an
   empty `OLD` when the branch must not exist yet), so the update fails instead of losing a
   commit if someone else moved the branch meanwhile.

Nothing else in `REPO` changes: not the index, not the working tree, not `HEAD` (if
`BRANCH` is the branch checked out, it moves, and `git status` then shows the difference, as
after `git commit-tree` and `git update-ref` by hand). `DIR` may be any folder, the
repository's own working tree included (its `.git` is never an entry).

**Nothing to commit.** When `DIR` has no files, or its tree is the same as the tree of the
branch's current tip, print `minigit.sh: nothing to commit` on stderr, exit `1` and write no
commit.

Errors: a wrong number of arguments, a `DIR` that is not a folder or a `REPO` that is not a
Git repository is the usage error (`2`).
