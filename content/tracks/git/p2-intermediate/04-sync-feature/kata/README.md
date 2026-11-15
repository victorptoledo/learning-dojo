# Sync a Feature Branch

Write `sync.sh REPO BRANCH`. `REPO` is a clone whose remote `origin` has a `main` branch.
The script brings the local branch `BRANCH` up to date with the server's `main` and
publishes it:

1. fetch `origin`;
2. rebase `BRANCH` onto `origin/main`, so the history is linear and `origin/main` is an
   ancestor of `BRANCH`;
3. push `BRANCH` to `origin` with `--force-with-lease` (a rebased branch needs a force) and
   set its upstream to `origin/BRANCH`;
4. print `BRANCH: N commits on top of origin/main`.

Afterwards `BRANCH` is checked out, and `origin`'s `main` is unchanged.

```text
$ bash sync.sh ~/shop feature/search
feature/search: 2 commits on top of origin/main
```

It refuses (message on **stderr**, exit `1`, nothing changed, nothing pushed) when:

| Situation | Message |
| --- | --- |
| a tracked file has uncommitted changes (untracked files are fine) | `sync.sh: uncommitted changes in REPO` |
| `BRANCH` is not a local branch | `sync.sh: no branch BRANCH` |
| `REPO` is not a repository | `sync.sh: not a git repository: REPO` |
| `origin/BRANCH` exists and has commits the local `BRANCH` does not contain (someone else pushed to it); check this right after the fetch | `sync.sh: origin/BRANCH has commits that BRANCH does not have` |
| the rebase hits a conflict: abort it, so `BRANCH` keeps its old commits and no rebase is left in progress | `sync.sh: BRANCH conflicts with origin/main` |

Wrong number of arguments: `usage: sync.sh REPO BRANCH` on stderr, exit `2`.

Why the fourth check? `--force-with-lease` compares the server with your `origin/BRANCH`,
and the fetch in step 1 just updated it. Without the check, the lease would happily
overwrite a colleague's push you never looked at.

Commands: `git fetch`, `git status --porcelain --untracked-files=no`, `git show-ref --verify`,
`git rev-parse -q --verify`, `git merge-base --is-ancestor`, `git rebase UPSTREAM BRANCH`,
`git rebase --abort`, `git push --force-with-lease -u`, `git rev-list --count`.

There are 11 tests: 1 in `tests/visible.bats` and 10 hidden ones, plus ShellCheck.
