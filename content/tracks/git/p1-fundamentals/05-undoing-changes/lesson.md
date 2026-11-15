# Undoing Local Changes: restore, reset and revert

"Undo" in Git depends on **where** the change is (working tree, index, a commit) and on
whether anyone else **has the commit already**. Pick the tool by answering those two
questions.

| I want to undo | Command | Safe on shared history? |
| --- | --- | --- |
| edits in the working tree | `git restore FILE` | yes (nothing shared) |
| a staged change | `git restore --staged FILE` | yes |
| untracked files | `git clean -n` then `git clean -f` | yes |
| the last commit, keep its changes | `git reset --soft HEAD~1` | **no**: rewrites |
| commits, back to a point | `git reset --hard <commit>` | **no**: rewrites |
| a pushed commit | `git revert <commit>` | **yes**: adds a commit |

## restore: files

```bash
git restore src/cart.py              # working tree back to the index version (edits are lost)
git restore --staged src/cart.py     # index back to HEAD (unstage); the edit stays on disk
git restore --source=HEAD~3 cart.py  # the file as it was three commits ago
git restore -p src/cart.py           # discard hunk by hunk
```

Edits discarded with `git restore` were never committed, so Git cannot bring them back. Look
before you leap: `git diff FILE`.

`git clean` deletes **untracked** files. Always run `git clean -n` (dry run) first;
`-d` includes directories and `-x` also removes ignored files such as build output.

## reset: move the branch

`git reset <commit>` moves the **current branch** to `<commit>`. The mode says how much else
follows:

| Mode | Branch | Index | Working tree | Use it to |
| --- | --- | --- | --- | --- |
| `--soft` | moves | kept | kept | squash the last commits into one, rewrite a message |
| `--mixed` (default) | moves | reset to the commit | kept | unstage everything, re-split commits |
| `--hard` | moves | reset | **reset** | throw commits and edits away |

```text
A - B - C - D   main          git reset --soft B
                               -> main at B; C and D's changes are staged
                               -> git commit makes one commit of C+D
```

`--hard` is the only command here that destroys uncommitted work: check `git status` and
`git stash` first. The commits you "removed" still exist and are listed by `git reflog`
for a while (Phase 2), so a wrong `reset --hard` of **commits** is recoverable; a wrong one
over **uncommitted edits** is not.

`git reset FILE` (with a path) is the old spelling of `git restore --staged FILE`.

## revert: undo by adding

```bash
git revert a1b2c3d          # a new commit applying the inverse of a1b2c3d
git revert --no-edit HEAD   # undo the last commit, default message
git revert -m 1 <merge>     # undo a merge: keep parent 1 (the branch you merged into)
git revert A..B             # several commits, one revert commit each
```

Revert does not rewrite anything, so it is the right tool for commits **others already
have**: `main`, a released tag, a pushed branch someone else uses. Reverting a merge has a
catch: if the branch is later fixed and merged again, Git thinks its old commits are
already in, so you must **revert the revert** first.

## Amend: fix the last commit

```bash
git commit --amend                 # new message, staged changes added
git commit --amend --no-edit       # add forgotten files, keep the message
git commit --amend --reset-author  # after fixing user.email
```

Amend creates a **new** commit and moves the branch to it (a small rewrite), so the same
rule applies: fine before pushing, a force-push after.

## The golden rule

**Do not rewrite commits that others have built on.** Locally, reset and amend are everyday
tools. Once a commit is on a shared branch, undo it with `revert`.

## Summary

- `restore` for files: the working tree from the index, or the index from `HEAD`.
- `reset` moves the branch; `--soft` keeps index and tree, `--mixed` keeps the tree,
  `--hard` keeps nothing.
- `revert` adds an inverse commit, safe for shared history; reverting a merge needs `-m`.
- Uncommitted work discarded by `restore` or `reset --hard` cannot be recovered; commits can.

## Further reading

- Pro Git, "Undoing Things": <https://git-scm.com/book/en/v2/Git-Basics-Undoing-Things>
- Pro Git, "Reset Demystified": <https://git-scm.com/book/en/v2/Git-Tools-Reset-Demystified>
- git-restore reference: <https://git-scm.com/docs/git-restore>
- git-revert reference: <https://git-scm.com/docs/git-revert>
- Atlassian, "Undoing changes": <https://www.atlassian.com/git/tutorials/undoing-changes>
- roadmap.sh topic "Undoing Changes": <https://github.com/nilbuild/developer-roadmap/blob/master/roadmaps/git-github/content/undoing-changes@0Yi4cryT2v2SGBjouOas3.md>
