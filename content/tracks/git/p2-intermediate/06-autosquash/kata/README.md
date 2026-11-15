# Tidy a Branch

Write `tidy.sh REPO BASE`. On the branch checked out in `REPO`, it folds every commit between
`BASE` and `HEAD` whose subject starts with `fixup! `, `squash! ` or `amend! ` into the
commit it targets, without any editor:

```text
before (git log --oneline main..HEAD)     after
e5 fixup! Add CSV header                   c3 Add totals
d4 Add totals                              b2 Add CSV header   (with e5 folded in)
c3 fixup! Add export                       a1 Add export       (with c3 folded in)
b2 Add CSV header
a1 Add export
```

```text
$ bash tidy.sh ~/shop main
folded 2 commits
```

Rules:

- the commits are folded **in place**: the branch stays on its current merge base with
  `BASE`, even if `BASE` has new commits (do not move the branch onto them);
- the final content does not change: the new `HEAD` has the same tree as the old one;
- a `squash!` commit keeps the target's subject as the first line of the combined message;
- with nothing to fold, print `nothing to fold` and leave `HEAD` exactly where it was (same
  hash).

It refuses (message on **stderr**, exit `1`, `HEAD` unchanged, no rebase left in progress):

| Situation | Message |
| --- | --- |
| a tracked file has uncommitted changes | `tidy.sh: uncommitted changes in REPO` |
| `BASE` does not name a commit | `tidy.sh: unknown base BASE` |
| folding hits a conflict (abort the rebase) | `tidy.sh: cannot fold the fixups without conflicts` |
| `REPO` is not a repository | `tidy.sh: not a git repository: REPO` |

Wrong number of arguments: `usage: tidy.sh REPO BASE` on stderr, exit `2`.

Commands: `GIT_SEQUENCE_EDITOR`, `GIT_EDITOR`, `git rebase -i --autosquash --keep-base`,
`git rebase --abort`, `git log --format=%s A..B`, `grep -c -E`, `git rev-parse -q --verify`.

There are 11 tests: 1 in `tests/visible.bats` and 10 hidden ones, plus ShellCheck.
