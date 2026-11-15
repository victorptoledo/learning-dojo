# Rebase vs Merge and the Reflog Safety Net

Both `merge` and `rebase` bring the work of one branch together with another. They differ in
the **history** they leave behind.

## What rebase does

`git rebase main` (run on `feature`) takes the commits of `feature` that are not on `main`,
and **replays** them one by one on top of `main`'s tip. Each replayed commit has a new
parent, so it is a **new commit with a new hash**. Then `feature` is moved to the last one.

```text
before:            C - D        feature
                  /
           A - B - E            main

git merge main (on feature):    git rebase main (on feature):
           C - D - M  feature              C' - D'  feature
          /       /                       /
   A - B - E -----     main        A - B - E        main
```

After the rebase, `main` is an ancestor of `feature`, so `git switch main && git merge
--ff-only feature` simply moves `main`: a **linear history**. The old `C` and `D` still
exist, unreferenced, until garbage collection.

Rebase **stops at each conflict**: fix the files, `git add` them, `git rebase --continue`
(or `--skip` that commit, or `--abort` to go back to where you started).

## Choosing

| | Merge commit | Rebase, then fast-forward | Squash merge |
| --- | --- | --- | --- |
| History | true shape, branches visible | straight line of the branch commits | one commit per branch |
| Commits rewritten | none | the branch's commits | the branch is replaced by one new commit |
| Revert a whole feature | `git revert -m 1 <merge>` | revert each commit (or a range) | `git revert <one commit>` |
| Bisect | works, may land on merges | easiest: every commit on a line | coarse: whole features |
| Conflicts | resolved once, in the merge | resolved per replayed commit | resolved once |

There is no universally right answer. Common team setups: **rebase locally to stay up to
date, merge (or squash-merge) through a pull request**, and keep `main` linear or at least
readable. GitHub offers all three on the merge button.

## The golden rule of rebasing

**Never rebase commits that other people have already based work on.** Rebasing replaces
commits with new ones. If a colleague built on the old ones, their history and yours now
disagree, and the next pull duplicates commits or produces confusing conflicts.

Rebasing **your own** feature branch is fine, even after pushing it, as long as nobody else
works on it: push with `git push --force-with-lease` (next lesson), never plain `--force`.

## pull: merge or rebase?

`git pull` is `git fetch` followed by `git merge` (the default) or `git rebase`:

```bash
git config --global pull.rebase true   # replay your local commits on top of what you fetched
git config --global pull.ff only       # or: refuse anything but a fast-forward
git pull --rebase                      # once
```

With `pull.rebase true`, pulling never creates "Merge branch 'main' of github.com:..."
commits, which are noise in most histories.

## The reflog: your safety net

Every time a branch or `HEAD` moves (commit, reset, rebase, switch, merge, amend), Git
writes a line in the **reflog** (`.git/logs/`). It is local, never pushed, and kept for
90 days by default (30 for unreachable entries).

```text
$ git reflog
4f1c2d9 (HEAD -> feature) HEAD@{0}: rebase (finish): returning to refs/heads/feature
4f1c2d9 HEAD@{1}: rebase (pick): Add search box
a7e5b10 HEAD@{2}: rebase (start): checkout main
9b3e8c1 HEAD@{3}: commit: Add search box
```

`HEAD@{3}` is where you were before the rebase started. To undo the whole rebase:

```bash
git reset --hard HEAD@{3}     # or the hash 9b3e8c1
git reset --hard ORIG_HEAD    # rebase, merge and reset store the previous tip in ORIG_HEAD
git reflog show feature       # the reflog of one branch
git log -g --oneline          # the reflog as a log
```

A rebase gone wrong, an accidental `reset --hard` to an older commit, a deleted branch: as
long as the work was **committed**, the reflog can find it.

## Summary

- Rebase replays commits as new commits on a new base, giving a linear history; merge keeps
  the original commits and records a merge commit.
- Squash merge collapses a branch into one commit: easy to revert, coarse to bisect.
- Never rebase commits others have built on; force-push your own branches with
  `--force-with-lease`.
- The reflog records every move of `HEAD` and branches; `reset --hard HEAD@{n}` or
  `ORIG_HEAD` undoes a bad rebase.

## Further reading

- Pro Git, "Rebasing": <https://git-scm.com/book/en/v2/Git-Branching-Rebasing>
- git-rebase reference: <https://git-scm.com/docs/git-rebase>
- git-reflog reference: <https://git-scm.com/docs/git-reflog>
- Atlassian, "Merging vs. Rebasing": <https://www.atlassian.com/git/tutorials/merging-vs-rebasing>
- GitHub Docs, "About merge methods on GitHub": <https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/configuring-pull-request-merges/about-merge-methods-on-github>
- roadmap.sh topic "git reflog": <https://github.com/nilbuild/developer-roadmap/blob/master/roadmaps/git-github/content/git-reflog@hru0sL1-D4bJSQI1efEyU.md>
