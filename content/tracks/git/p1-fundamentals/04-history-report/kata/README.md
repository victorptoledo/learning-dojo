# History Report

Write `gitstats.sh REPO`. It prints a summary of the history reachable from `HEAD` in the
repository `REPO`, exactly in this layout:

```text
commits: 5
authors: 2
first: Initial commit
last: Add tax rules
top files:
3 src/cart.sh
2 README.md
1 docs/cart.md
```

| Line | Meaning |
| --- | --- |
| `commits` | commits reachable from `HEAD`, merge commits included; other branches do not count |
| `authors` | number of **different author e-mail addresses** among those commits |
| `first` | subject of the oldest commit (the last one `git log` lists) |
| `last` | subject of the commit `HEAD` points to |
| `top files` | the 3 files changed by the most commits, as `COUNT PATH`, most changed first; equal counts in alphabetical order of the path; fewer lines if fewer files. Merge commits are not counted here |

A repository **without commits** prints:

```text
commits: 0
authors: 0
first: -
last: -
top files:
```

Errors:

- no argument or more than one: print `usage: gitstats.sh REPO` to **stderr**, exit `2`;
- `REPO` is not a Git repository: print `gitstats.sh: not a git repository: REPO` to stderr
  (with the name given) and exit `1`.

Commands: `git -C`, `git rev-parse --git-dir`, `git rev-parse --verify -q HEAD`,
`git rev-list --count`, `git log --format=%ae / %s`, `git log --no-merges --format=
--name-only`, `sort`, `uniq -c`, `sort -k1,1nr -k2,2`, `tail`.

There are 10 tests: 1 in `tests/visible.bats` and 9 hidden ones, plus ShellCheck.
