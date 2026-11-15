# Resolving Merge and Rebase Conflicts

A **conflict** happens when both sides of a three-way merge changed the **same region** of a
file differently (or one side changed a file the other deleted). Git cannot guess the
intent, so it stops and asks you. A conflict is not an error; it is a question.

## What Git leaves you

```text
$ git merge feature
Auto-merging pricing.py
CONFLICT (content): Merge conflict in pricing.py
Automatic merge failed; fix conflicts and then commit the result.

$ git status --short
UU pricing.py
```

Inside the file, the conflicting region is wrapped in markers. Turn on the **zdiff3** style
once and Git also shows what the **merge base** had, which is usually what you need to
understand both intents:

```bash
git config --global merge.conflictStyle zdiff3
```

```text
<<<<<<< HEAD
DISCOUNT = discount_rate(customer)
||||||| base
DISCOUNT = 0.10
=======
DISCOUNT = 0.15
>>>>>>> feature
```

Read it as: the base had `0.10`; our side turned it into a function call; their side raised
it to `0.15`. The right answer keeps both intents, for example by changing the rate inside
`discount_rate()`. Picking one side would lose a change.

## Resolving

```bash
# edit the file, remove every marker, make it correct
git add pricing.py        # marks it resolved
git merge --continue      # or git commit; for a rebase: git rebase --continue
git merge --abort         # give up and go back to before the merge
git diff                  # during a conflict: a combined diff of what is still unresolved
git checkout --ours FILE  # take our whole version of a file
git checkout --theirs FILE
git mergetool             # open a configured three-way tool (VS Code, IntelliJ, meld...)
```

After resolving, **build and run the tests**. A textually clean merge can still be
semantically wrong: one side renamed a function that the other side started calling.

## Ours and theirs

| Operation | `ours` (HEAD) | `theirs` |
| --- | --- | --- |
| `git merge feature` on `main` | `main` | `feature` |
| `git rebase main` on `feature` | `main` (plus the commits already replayed) | the `feature` commit being replayed |
| `git cherry-pick X` | the current branch | `X` |

During a rebase the sides look swapped, because Git checks out `main` and replays your
commits onto it: **your** commit is "theirs". Many people get this wrong once.

## Conflicts you meet often

| Kind | What to do |
| --- | --- |
| both edited the same lines | combine the intents by hand |
| modify/delete | decide: `git rm FILE` to keep the deletion, or `git add FILE` to keep it |
| rename on one side, edit on the other | usually auto-merged by rename detection; check the result |
| lock files (`package-lock.json`, `poetry.lock`) | take one side, regenerate with the package manager, add |
| generated files | do not resolve by hand: regenerate |
| binary files | choose one with `--ours`/`--theirs`; Git cannot merge them |

## rerere: reuse recorded resolution

If you rebase a long branch several times, you resolve the same conflict again and again.
`rerere` remembers how you resolved a conflict and replays it the next time the same one
appears:

```bash
git config --global rerere.enabled true
```

## Fewer conflicts

- **Integrate often**: small branches that live for days, not weeks, conflict less.
- Rebase onto (or merge) `main` regularly while you work.
- Keep mechanical changes (formatting, renames, moving files) in separate commits and land
  them quickly, so they do not collide with feature work.
- Agree on a formatter, so the same code is never "conflicting" only by whitespace.

## Summary

- A conflict means both sides changed the same region; Git asks you for the intent.
- Use `zdiff3` to see the base; resolve, `git add`, then `--continue` (or `--abort`).
- In a rebase, `ours` is the branch you rebase onto and `theirs` is your replayed commit.
- Run the tests after resolving; enable `rerere`; integrate small and often.

## Further reading

- Pro Git, "Advanced Merging": <https://git-scm.com/book/en/v2/Git-Tools-Advanced-Merging>
- Pro Git, "Rerere": <https://git-scm.com/book/en/v2/Git-Tools-Rerere>
- git-merge reference, "How conflicts are presented": <https://git-scm.com/docs/git-merge>
- GitHub Docs, "Resolving a merge conflict using the command line": <https://docs.github.com/en/pull-requests/how-tos/merge-and-close-pull-requests/resolving-a-merge-conflict-using-the-command-line>
- Atlassian, "Git merge conflicts": <https://www.atlassian.com/git/tutorials/using-branches/merge-conflicts>
- roadmap.sh topic "Handling Conflicts": <https://github.com/nilbuild/developer-roadmap/blob/master/roadmaps/git-github/content/handling-conflicts@9Dt4Gyiiq5jteWSKxsRMK.md>
