# Regression Hunt

Write `hunt.sh REPO GOOD CMD`. `GOOD` names a commit where things worked (a hash, a tag, a
branch), `HEAD` is where they are broken, and `CMD` is a shell command line that tells them
apart. The script finds the **first bad commit** between `GOOD` and `HEAD` with `git bisect`
and prints its full hash and subject:

```text
$ bash hunt.sh ~/shop v2.3.0 'grep -q "total: 41.50" receipt.txt'
3f9a2c1e0b7d4a5f8c2e9d1b6a3f7e0c4d8b2a19 Round prices per line
```

`CMD` runs with `bash -c CMD` in the root of the working tree of the commit being tested.
Its exit code means, as for `git bisect run`: `0` good, `125` cannot be tested (skip), any
other code from 1 to 127 bad. Only the result line goes to stdout.

Afterwards no bisect is in progress, the branch (or detached commit) that was checked out
before is checked out again and the working tree is clean.

It refuses (message on **stderr**, exit `1`, repository unchanged) when:

| Situation | Message |
| --- | --- |
| `REPO` is not a repository | `hunt.sh: not a git repository: REPO` |
| a tracked file has uncommitted changes | `hunt.sh: uncommitted changes in REPO` |
| `GOOD` does not name a commit | `hunt.sh: unknown commit GOOD` |
| `CMD` passes at `HEAD` | `hunt.sh: HEAD passes the test, nothing to hunt` |
| `CMD` does not pass at `GOOD` | `hunt.sh: GOOD fails the test too` |

Wrong number of arguments: `usage: hunt.sh REPO GOOD CMD` on stderr, exit `2`.

Commands: `git bisect start BAD GOOD`, `git bisect run`, `git rev-parse refs/bisect/bad`,
`git bisect reset`, `git symbolic-ref -q --short HEAD`, `git checkout --detach`,
`git log -1 --format='%H %s'`.

There are 11 tests: 1 in `tests/visible.bats` and 10 hidden ones, plus ShellCheck.
