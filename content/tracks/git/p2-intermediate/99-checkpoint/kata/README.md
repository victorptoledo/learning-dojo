# Land a Feature Branch

Write `land.sh REPO BRANCH`: the "rebase and merge" button of a hosting service, as a
script. `REPO` is a clone whose remote `origin` has a `main` branch. The script:

1. fetches `origin`;
2. folds the `fixup!`, `squash!` and `amend!` commits of `BRANCH` and rebases it onto
   `origin/main`, in one non-interactive rebase;
3. moves `main` **on the server** to the rebased branch (a fast-forward: no merge commit);
4. brings the local `main` to the same commit and checks it out;
5. deletes `BRANCH` locally and, if it exists there, on `origin`;
6. prints `landed BRANCH: N commits on main`, `N` being the commits that landed after
   folding.

```text
$ bash land.sh ~/shop feature/export
landed feature/export: 2 commits on main
```

It refuses (message on **stderr**, exit `1`) and pushes nothing when:

| Situation | Message |
| --- | --- |
| `REPO` is not a repository | `land.sh: not a git repository: REPO` |
| a tracked file has uncommitted changes | `land.sh: uncommitted changes in REPO` |
| `BRANCH` is not a local branch, or is `main` | `land.sh: no branch to land: BRANCH` |
| after the fetch, `origin/BRANCH` has commits `BRANCH` does not have | `land.sh: origin/BRANCH has commits that BRANCH does not have` |
| after the fetch, local `main` has commits `origin/main` does not have | `land.sh: local main has commits that origin/main does not have` |
| the rebase conflicts: abort it, `BRANCH` keeps its commits | `land.sh: BRANCH conflicts with origin/main` |
| after the rebase nothing is left on top of `origin/main` | `land.sh: BRANCH has nothing to land` |

Wrong number of arguments: `usage: land.sh REPO BRANCH` on stderr, exit `2`.

Commands: everything from Phase 2: `fetch`, `merge-base --is-ancestor`,
`rebase -i --autosquash` with `GIT_SEQUENCE_EDITOR`, `rebase --abort`,
`push origin SRC:DST`, `merge --ff-only`, `branch -d`, `push origin --delete`.

There are 11 tests: 1 in `tests/visible.bats` and 10 hidden ones, plus ShellCheck.
