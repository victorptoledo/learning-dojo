# Cut a Hotfix Release

Release `v1.1.9` is broken; `v1.0.0` was fine. Write `hotfix.sh REPO GOOD BAD TEST`, the
script a release manager runs to ship the fix without touching anything else. `GOOD` and
`BAD` are release tags of `REPO`; `TEST` is a shell command run with `bash -c` at the root of a
checkout: exit `0` means good, `125` means "this commit cannot be tested", anything else
means bad. The script:

1. checks both tags: each must be an **annotated** tag named `vMAJOR.MINOR.PATCH`, and
   `GOOD` must be an ancestor of `BAD`;
2. works in a **worktree of its own** on a new branch `hotfix/NEXT`, started at `BAD`, where
   `NEXT` is `BAD` with the patch number plus one (`v1.1.9` gives `v1.1.10`);
3. checks the ends there: `TEST` must fail at `BAD` and pass at `GOOD`;
4. finds the first bad commit with `git bisect`, skipping what `TEST` cannot check;
5. reverts that commit on `hotfix/NEXT` with a commit that goes through the repository's
   `pre-commit` and `commit-msg` hooks (no `--no-verify`, and watch out: `git revert` on its
   own does not run them). The message is Git's own: `Revert "SUBJECT"`;
6. runs `TEST` again on the hotfix, which must now pass;
7. tags it with an annotated tag `NEXT` whose message is `Hotfix NEXT`;
8. removes its worktree, keeps the branch and the tag, and prints:

```text
$ bash hotfix.sh ~/shop v1.0.0 v1.1.9 'grep -qx "total=42" total.txt'
culprit: 3f2c9e0d4b7a8c1e5f6a2b3c4d5e6f7a8b9c0d1e Round prices
tagged v1.1.10 on hotfix/v1.1.10
```

The user's checkout is never touched: not their branch, not their uncommitted or untracked
files, not a bisect they may have going on in it.

It refuses (message on **stderr**, exit `1`) and, once it has started, leaves **nothing**
behind (no worktree, no `hotfix/` branch, no tag) when:

| Situation | Message |
| --- | --- |
| `REPO` is not a repository | `hotfix.sh: not a git repository: REPO` |
| a tag is missing, lightweight or not `vX.Y.Z` | `hotfix.sh: TAG is not an annotated vX.Y.Z tag` |
| `GOOD` is not an ancestor of `BAD` | `hotfix.sh: GOOD is not an ancestor of BAD` |
| the tag `NEXT` or the branch `hotfix/NEXT` exists | `hotfix.sh: NEXT already exists` |
| `TEST` passes at `BAD` | `hotfix.sh: BAD passes the test, nothing to fix` |
| `TEST` fails at `GOOD` | `hotfix.sh: GOOD fails the test` |
| the revert conflicts or a hook rejects it | `hotfix.sh: could not revert HASH` |
| `TEST` still fails on the hotfix | `hotfix.sh: the test still fails after reverting HASH` |

`HASH` is the culprit's full hash. Wrong number of arguments: `usage: hotfix.sh REPO GOOD BAD
TEST` on stderr, exit `2`.

Commands: everything from Phase 3 and a little from before: `cat-file -t`,
`merge-base --is-ancestor`, `worktree add -b`, `worktree remove`, `bisect start`, `bisect run`,
`bisect reset`, `revert --no-commit`, `commit --no-edit`, `tag -a`, and `trap` for the cleanup.

There are 16 tests: 1 in `tests/visible.bats` and 15 hidden ones, plus ShellCheck.
