# Branches, HEAD and Merging

A branch is a **name that moves**. It holds the hash of one commit, its **tip**. When you
commit on a branch, the new commit gets the old tip as its parent and the branch moves to
the new commit. Nothing else happens: no files are copied.

## Creating and switching

```bash
git branch                       # list local branches; * marks the current one
git branch -vv                   # with tips and upstreams
git switch -c feature/search     # create and switch (older: git checkout -b)
git switch main                  # switch (older: git checkout main)
git switch -                     # back to the previous branch
git branch -m feature/find       # rename the current branch
git branch -d feature/find       # delete, only if it is merged
git branch -D experiment         # delete even if it is not merged
```

`git switch` updates three things: `HEAD` (now `ref: refs/heads/main`), the index and the
working tree, to match the tip of the target. Uncommitted changes come along if they do not
clash with files that differ between the branches; otherwise Git refuses, so you never lose
them silently.

`git checkout` is the old Swiss army knife (switch branches **and** restore files). Since
Git 2.23 `git switch` and `git restore` split those jobs; prefer them.

## Setting work aside: stash

```bash
git stash push -m "half-done search"   # save working tree and index, clean the tree
git stash list                          # stash@{0}: On feature/search: half-done search
git stash pop                           # reapply the newest and drop it
git stash apply stash@{1}               # reapply without dropping
git stash push -u                       # include untracked files
```

A stash is a commit kept on a special ref (`refs/stash`). A small WIP commit on your branch
does the same job and is harder to forget.

## Detached HEAD

`git switch --detach v1.2.0` or `git checkout a1b2c3d` puts a **hash** in `HEAD` instead of
a branch name. You can look around and even commit, but no branch moves. When you switch
away, those commits are reachable from no name and will eventually be garbage-collected.
To keep them: `git switch -c keep-this` **before** leaving (or later, through the reflog).

## Merging

`git merge feature` brings the commits of `feature` into the **current** branch. There are
two outcomes.

### Fast-forward

If the current branch is an **ancestor** of `feature` (it has not moved since `feature` was
created), there is nothing to combine: Git just moves the branch forward.

```text
before:  A - B          main
              \
               C - D    feature
after:   A - B - C - D  main, feature
```

### Three-way merge

If both branches have new commits, Git finds their **merge base** (the best common
ancestor), compares each side with it, combines the changes and records a **merge commit**
with two parents.

```text
before:  A - B - E        main
              \
               C - D      feature
after:   A - B - E - M    main     (M has parents E and D)
              \     /
               C - D      feature
```

"Three-way" because three snapshots are compared: the base, ours and theirs. A line changed
on one side only takes that side; a line changed on both sides is a **conflict** (Phase 2).

```bash
git merge-base main feature   # the common ancestor
git merge --ff-only feature   # refuse if a fast-forward is not possible
git merge --no-ff feature     # always create a merge commit, to keep the branch visible
git merge --abort             # give up a merge in progress
git branch --merged main      # branches whose tips are already in main
```

Teams choose: `--no-ff` keeps a visible "this feature landed here" commit; `--ff-only`
(often after a rebase, Phase 2) keeps history linear.

## Branch naming

Branch names are paths: `feature/search`, `fix/vat-rounding`, `release/2.4`. A ticket number
helps (`fix/SHOP-142-vat-rounding`). Avoid spaces, `..`, a leading `-` and names that differ
only by case (Windows and macOS file systems ignore case).

## Summary

- A branch is a moving pointer to a tip; committing moves the current branch, `HEAD` says
  which branch is current.
- `git switch` changes branch and refuses to overwrite uncommitted work; `git stash` or a WIP
  commit sets work aside.
- Detached `HEAD` points at a commit directly; create a branch before leaving it.
- A merge is a fast-forward when the current branch is an ancestor of the other, otherwise a
  three-way merge creates a merge commit from the merge base, ours and theirs.

## Further reading

- Pro Git, "Branches in a Nutshell": <https://git-scm.com/book/en/v2/Git-Branching-Branches-in-a-Nutshell>
- Pro Git, "Basic Branching and Merging": <https://git-scm.com/book/en/v2/Git-Branching-Basic-Branching-and-Merging>
- git-switch reference: <https://git-scm.com/docs/git-switch>
- git-merge reference: <https://git-scm.com/docs/git-merge>
- Atlassian, "Using branches": <https://www.atlassian.com/git/tutorials/using-branches>
- roadmap.sh topic "Fast-Forward vs Non-FF": <https://github.com/nilbuild/developer-roadmap/blob/master/roadmaps/git-github/content/fast-forward-vs-non-ff@agtPWS8j6i6wQPk10cy8E.md>
