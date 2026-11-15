# Reflog Rescue

Write `rescue.sh REPO BRANCH`. It undoes the most recent mistake that happened to `BRANCH`,
using only the reflogs.

**If `BRANCH` exists**, find the newest entry of its own reflog whose message starts with one
of these (each is a move that can lose commits):

| Prefix | Comes from |
| --- | --- |
| `reset: ` | `git reset` |
| `branch: Reset to ` | `git branch -f` |
| `rebase (finish): ` | a finished rebase |

and move `BRANCH` back to where it was just **before** that entry (`BRANCH@{n+1}` when the
entry is `BRANCH@{n}`). If `BRANCH` is checked out, the working tree and index follow (refuse
when a tracked file has uncommitted changes); otherwise only the branch moves and the
current checkout is untouched.

**If `BRANCH` does not exist** (it was deleted), find the newest entry of `HEAD`'s reflog that
starts with `checkout: moving from BRANCH to ` and create `BRANCH` at the commit `HEAD` was on
just before that entry.

Then print `restored BRANCH to HASH SUBJECT` (full hash, subject of that commit):

```text
$ git reset --hard HEAD~3          # oops
$ bash rescue.sh . main
restored main to 9f8e7d6a1b2c3d4e5f60718293a4b5c6d7e8f901 Add tax rules
```

Your own move must **not** look like a new mistake: running `rescue.sh` twice in a row gives
the same result as running it once. (Hint: a reflog message that starts with `rescue: `.)

Errors (message on **stderr**, exit `1`, nothing changed):

| Situation | Message |
| --- | --- |
| `REPO` is not a repository | `rescue.sh: not a git repository: REPO` |
| `BRANCH` exists but no entry of its reflog matches | `rescue.sh: nothing to undo on BRANCH` |
| `BRANCH` does not exist and `HEAD`'s reflog never left it | `rescue.sh: no trace of BRANCH in the reflog` |
| `BRANCH` is checked out and a tracked file has uncommitted changes | `rescue.sh: uncommitted changes in REPO` |

Wrong number of arguments: `usage: rescue.sh REPO BRANCH` on stderr, exit `2`.

Commands: `git log -g --format=%gs REF`, `git rev-parse REF@{n}`, `git show-ref --verify`,
`git symbolic-ref -q --short HEAD`, `git update-ref -m`, `GIT_REFLOG_ACTION`,
`git reset --hard`, `git branch NAME HASH`, `awk 'index($0, prefix) == 1'`.

There are 12 tests: 1 in `tests/visible.bats` and 11 hidden ones, plus ShellCheck.
