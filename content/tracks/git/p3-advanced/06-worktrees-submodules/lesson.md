# Worktrees, Submodules and Subtrees

Two different problems, often confused: **one repository, several checkouts** (worktrees),
and **one repository inside another** (submodules and subtrees).

## Worktrees: several branches checked out at once

You are in the middle of a change and must review a pull request, or run a long test on
`main` while you keep coding. Instead of stashing or cloning again, add a **worktree**: a
second working directory that shares the same repository (objects, refs, config).

```bash
git worktree add ../shop-review feature/search     # check out an existing branch there
git worktree add -b hotfix/vat ../shop-hotfix main # new branch from main
git worktree list
# /home/ada/shop          3f9a2c1 [feature/export]
# /home/ada/shop-review   a7e5b10 [feature/search]
git worktree remove ../shop-review
git worktree prune                                  # forget worktrees whose folder was deleted
```

Rules worth knowing:

- A branch can be checked out in **one** worktree at a time (Git refuses a second one).
- Commits made in any worktree are immediately visible to all of them: it is one repository.
- Each worktree has its own `HEAD`, index and working files; its `.git` is a small file
  pointing back to the main repository (`gitdir: /home/ada/shop/.git/worktrees/shop-review`).
- Ignored build output is per folder, so a worktree needs its own build or `npm ci`.

## Submodules: a pinned pointer to another repository

A **submodule** puts another repository at a path inside yours and records **one exact
commit** of it. Your repository stores two things: a line in `.gitmodules` (path and URL)
and a special tree entry, a **gitlink** (mode `160000`), holding the commit hash.

```bash
git submodule add https://github.com/acme/proto.git proto
git commit -m "Add proto as a submodule"

git clone --recurse-submodules git@github.com:acme/shop.git   # clone with submodules
git submodule update --init --recursive                        # after a plain clone or a pull

cd proto && git switch main && git pull && cd ..   # move the submodule forward
git add proto && git commit -m "Bump proto"         # record the new commit (the gitlink)

git submodule status                                # the commit each submodule is at
git config --global submodule.recurse true          # let pull/switch update them
```

The classic surprises:

- After `git pull`, the submodule does not move until `git submodule update` (unless
  `submodule.recurse` is set): `git status` shows `modified: proto (new commits)`.
- A submodule is checked out in **detached HEAD**; switch to a branch before committing in it.
- Changes in the submodule must be **pushed** to its own repository before you push the
  bump, or your teammates' `submodule update` fails to find the commit.

## Subtrees: a copy merged into your history

`git subtree` (a contrib command shipped with most Git installations) copies another
project's files into a folder of yours, **as ordinary files and commits**.

```bash
git subtree add --prefix=proto https://github.com/acme/proto.git main --squash
git subtree pull --prefix=proto https://github.com/acme/proto.git main --squash
git subtree push --prefix=proto https://github.com/acme/proto.git my-change
```

Clones need nothing special and every file is there, but updating and contributing back
take discipline, and the folder can drift from upstream if people edit it carelessly.

## Choosing

| | Submodule | Subtree | Package (Maven, npm, PyPI, Go module) |
| --- | --- | --- | --- |
| What your repository holds | a pointer (commit hash) | a copy of the files | a version number |
| Clone | needs `--recurse-submodules` | normal | normal, then install |
| Update | bump the gitlink | `subtree pull` | bump the version |
| Change the shared code | commit and push in the submodule, then bump | edit, then `subtree push` | release a new version |
| Good for | vendored repositories you rarely change, exact pinning | small shared code you want inside every clone | anything a package manager handles |

When the shared code has a package manager, a **versioned package** is usually the
simplest choice. Monorepos are the other alternative: one repository, no pointers at all
(Phase 4).

## Summary

- `git worktree add` gives you another checkout of the same repository; one branch per
  worktree; `remove` and `prune` clean up.
- A submodule records a commit of another repository (gitlink plus `.gitmodules`); clone
  with `--recurse-submodules`, update with `git submodule update`, bump by committing the
  new pointer.
- A subtree copies another project into a folder of your history; simpler to clone, harder
  to keep in sync.
- Prefer a versioned package when one fits.

## Further reading

- git-worktree reference: <https://git-scm.com/docs/git-worktree>
- Pro Git, "Submodules": <https://git-scm.com/book/en/v2/Git-Tools-Submodules>
- git-submodule reference: <https://git-scm.com/docs/git-submodule>
- Atlassian, "Git submodules": <https://www.atlassian.com/git/tutorials/git-submodule>
- Atlassian, "Git subtree": <https://www.atlassian.com/git/tutorials/git-subtree>
- roadmap.sh topic "Git Worktree": <https://github.com/nilbuild/developer-roadmap/blob/master/roadmaps/git-github/content/git-worktree@4dxVN81rXWn6VZqK99yq0.md>
- roadmap.sh topic "Submodules": <https://github.com/nilbuild/developer-roadmap/blob/master/roadmaps/git-github/content/submodules@fjAFNjNNbPOzme9Uk_fDV.md>
