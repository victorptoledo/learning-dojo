# Finding Regressions with git bisect

Something worked in `v2.3.0` and is broken in `v2.4.0`, with hundreds of commits in between.
Reading them all is hopeless; testing them all is slow. `git bisect` does a **binary
search**: each test halves the remaining range, so 1 000 commits take about 10 tests.

## A manual session

```bash
git bisect start
git bisect bad              # the current commit (HEAD) is broken
git bisect good v2.3.0      # this one was fine
# Git checks out a commit in the middle: test it, then tell Git
git bisect good             # or: git bisect bad
# ... repeat until Git prints:
# 3f9a2c1 is the first bad commit
git bisect reset            # back to the branch you started from
```

Shortcuts: `git bisect start BAD GOOD` in one line; `git bisect skip` when the current commit
cannot be tested; `git bisect log` to see the session and `git bisect replay FILE` to redo
it; `git bisect visualize` (or `view`) to see what is left. Terms do not have to be
"good/bad": `git bisect start --term-old=fast --term-new=slow` hunts for a performance
change.

## Automating with bisect run

If a command can decide whether a commit is good, Git can run the whole search alone:

```bash
git bisect start v2.4.0 v2.3.0
git bisect run ./check-totals.sh
git bisect reset
```

The **exit code** of the command decides:

| Exit code | Meaning |
| --- | --- |
| `0` | good |
| `1` to `127`, except `125` | bad |
| `125` | cannot test this commit: skip it |
| `128` or more | stop the bisect (something is badly wrong) |

A typical script builds, returns 125 if the build fails, then runs one focused test:

```bash
#!/usr/bin/env bash
make build > /dev/null 2>&1 || exit 125   # does not build: skip, it says nothing about the bug
./bin/shop checkout --cart fixtures/cart.json | grep -q "total: 41.50"
```

Write the test **outside** the repository (or untracked), so checking out old commits does not
change or remove it. A test that already exists in the repository's newest version can be
copied to a temporary folder first.

## Where it is found and what to do next

At the end Git prints the first bad commit; `refs/bisect/bad` points to it until you reset.
Inspect it with `git show`, write a regression test, fix it, and mention the culprit in the
fix's message.

## Bisect-friendly history

- **Small commits**: the culprit is then a few lines, not a 2 000-line "big refactor".
- **Every commit builds and passes the tests**: otherwise you skip a lot, and skipping next
  to the culprit makes the answer ambiguous ("the first bad commit could be any of ...").
  `git rebase -i --exec "make test"` checks a branch before it lands.
- **Merges**: bisect handles them, but it may first tell you a merge commit is guilty. With
  `git bisect start --first-parent` it only walks the main line, which answers "which merged
  feature broke it?" first.

## Summary

- `git bisect` binary-searches between a known good and a known bad commit: about
  log2(N) tests.
- `git bisect run CMD` automates it: exit 0 good, 125 skip, other 1-127 bad.
- Keep the test script outside the tracked files; end every session with `git bisect reset`.
- Small, always-building commits keep history bisectable; `--first-parent` finds the merge.

## Further reading

- Pro Git, "Debugging with Git": <https://git-scm.com/book/en/v2/Git-Tools-Debugging-with-Git>
- git-bisect reference: <https://git-scm.com/docs/git-bisect>
- git-bisect-lk2009, "Fighting regressions with git bisect": <https://git-scm.com/docs/git-bisect-lk2009>
- roadmap.sh topic "git bisect": <https://github.com/nilbuild/developer-roadmap/blob/master/roadmaps/git-github/content/git-bisect@ExXuwTQSI_lg4SRGW3Iu1.md>
