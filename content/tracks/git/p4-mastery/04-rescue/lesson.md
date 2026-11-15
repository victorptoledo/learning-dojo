# Recovering from Mistakes: a Reflog Rescue Tool

The good news first: **Git almost never loses a commit**. Objects are only deleted by garbage
collection, and only when nothing (no branch, tag, reflog entry or stash) has pointed at
them for weeks. Most "I lost everything" moments are a moved pointer, and moving it back is
one command once you know where it was.

What Git **cannot** bring back is work that was never committed or staged: edits thrown away
by `git restore`, `git checkout -- FILE`, `git reset --hard` or `git clean`.

## Stay calm, look first

```bash
git status                    # where am I? is a merge, rebase or bisect in progress?
git reflog                    # every move of HEAD, newest first
git reflog show feature       # every move of one branch
git log --oneline --graph --all -20
```

If an operation is still **in progress**, the way out is its own abort:
`git merge --abort`, `git rebase --abort`, `git cherry-pick --abort`, `git bisect reset`.

## The recovery table

| Mistake | Recovery |
| --- | --- |
| wrong message or a missing file in the last commit (not pushed) | `git commit --amend` |
| committed on `main` instead of a new branch (not pushed) | `git branch feature` then `git reset --hard origin/main` (the commits stay on `feature`) |
| `git reset --hard` to the wrong commit | `git reset --hard ORIG_HEAD`, or `git reflog` and `git reset --hard HEAD@{n}` |
| a rebase went wrong (finished) | `git reflog show BRANCH`: the entry before `rebase (finish)`, or `ORIG_HEAD` right after |
| deleted a branch | `git reflog`: find `checkout: moving from BRANCH to ...`, then `git branch BRANCH HEAD@{n+1}` (Git also prints the tip's hash when it deletes) |
| commits made in detached HEAD, then switched away | `git reflog`, then `git branch rescued <hash>` |
| dropped a stash | `git fsck --unreachable \| grep commit`, or the hash `git stash drop` printed; `git stash apply <hash>` |
| staged a file, then lost the edit | `git fsck --lost-found`: staged content exists as a dangling blob |
| merged the wrong branch (not pushed) | `git reset --hard ORIG_HEAD` (or `--merge` to keep unrelated local edits) |
| pushed a bad commit to a shared branch | `git revert <hash>` and push (never rewrite shared history) |
| force-pushed over a colleague's commits | their clone (or yours) still has them: `git reflog` there, push them back; hosts also keep recent push history |
| committed a secret | rotate it first, then rewrite (previous lesson) |

`ORIG_HEAD` is set by `reset`, `merge` and `rebase` to the commit `HEAD` was at before; the
reflog goes back much further (90 days by default, 30 for unreachable commits).

## Reading the reflog precisely

Each branch has its own reflog, and `HEAD` has one too:

```text
$ git reflog show main
a1b2c3d main@{0}: reset: moving to HEAD~3
9f8e7d6 main@{1}: commit: Add tax rules
5c4b3a2 main@{2}: commit: Add cart
```

`main@{0}` is where `main` is now, and the entry says how it got there. `main@{1}` is where it
was **before** that move: the place to go back to. Typical messages:

| Message starts with | Written by |
| --- | --- |
| `commit:`, `commit (amend):`, `commit (merge):` | `git commit` |
| `reset: moving to ...` | `git reset` |
| `branch: Reset to ...` | `git branch -f` |
| `rebase (finish): ...` | the end of a rebase (on the branch's own reflog) |
| `checkout: moving from A to B` | `git switch` / `git checkout` (on HEAD's reflog) |
| `merge X: Fast-forward` | `git merge` |

When a branch is **deleted**, its reflog is deleted with it. `HEAD`'s reflog survives, and
every time you left that branch it wrote `checkout: moving from BRANCH to ...`; the entry just
before it, `HEAD@{n+1}`, is the branch's tip at that moment.

Scripts can read reflogs with `git log -g --format=%gs REF` (messages) and
`--format=%H` (hashes), and name entries as `REF@{n}`.

## The kata

Press **Enter** to create the kata folder and open it. You will write `rescue.sh`, which
automates the three most common recoveries in that table: undoing a bad reset, a bad
`branch -f` or a bad rebase, and bringing back a deleted branch.

## Summary

- Committed work is almost never lost; uncommitted edits are.
- Abort operations in progress with their own `--abort`; after they finish, use `ORIG_HEAD`
  or the reflog.
- A branch's reflog says where it was before each move (`BRANCH@{n+1}`); `HEAD`'s reflog
  remembers deleted branches through its `checkout: moving from` entries.
- Shared history is repaired with `revert`, never with a rewrite.

## Further reading

- git-reflog reference: <https://git-scm.com/docs/git-reflog>
- Pro Git, "Maintenance and Data Recovery": <https://git-scm.com/book/en/v2/Git-Internals-Maintenance-and-Data-Recovery>
- git-fsck reference (--lost-found, --unreachable): <https://git-scm.com/docs/git-fsck>
- Atlassian, "Git reflog": <https://www.atlassian.com/git/tutorials/rewriting-history/git-reflog>
- roadmap.sh topic "git reset": <https://github.com/nilbuild/developer-roadmap/blob/master/roadmaps/git-github/content/git-reset@igrR7LguU1jemg_mf_AD6.md>
