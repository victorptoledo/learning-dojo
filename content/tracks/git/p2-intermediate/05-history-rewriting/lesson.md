# Rewriting History: Interactive Rebase, Fixup and Autosquash

Commits are written for the next person who reads them. While a branch is still **yours**,
you can edit its history until it tells a clear story: one logical change per commit, in an
order that makes sense, with good messages. The tool is the interactive rebase.

## The todo list

`git rebase -i main` (or `git rebase -i HEAD~4`) opens your editor with one line per commit,
**oldest first**:

```text
pick 1a2b3c4 Add order export
pick 5d6e7f8 WIP
pick 9a0b1c2 fix typo
pick 3d4e5f6 Add CSV header

# Commands:
# p, pick   = use commit
# r, reword = use commit, but edit the message
# e, edit   = use commit, but stop for amending (or splitting)
# s, squash = meld into previous commit, combine the messages
# f, fixup  = meld into previous commit, keep only the previous message
# d, drop   = remove commit
```

Change the verbs, reorder the lines, save and close. Git replays the list top to bottom:

```text
pick 1a2b3c4 Add order export
fixup 5d6e7f8 WIP
fixup 9a0b1c2 fix typo
reword 3d4e5f6 Add CSV header
```

The result: two commits, the first containing the WIP and typo fixes, the second with a new
message. Every rewritten commit has a new hash, so push with `--force-with-lease`.

If a step conflicts, resolve it as in the conflicts lesson and `git rebase --continue`.
`git rebase --abort` returns to the state before you started, and after it finished,
`git reset --hard ORIG_HEAD` (or the reflog) undoes it.

## Fixup commits and autosquash

Review comments arrive after the branch is clean. Instead of piling up "address review"
commits, make each fix a **fixup commit** aimed at the commit it corrects:

```bash
git add -p
git commit --fixup=1a2b3c4        # creates "fixup! Add order export"
git commit --squash=3d4e5f6       # "squash! Add CSV header": also lets you edit the message
git commit --fixup=reword:1a2b3c4 # "amend! ...": replace the target's message
git rebase -i --autosquash main   # the todo list is pre-arranged: each fixup under its target
```

With `git config --global rebase.autoSquash true`, every interactive rebase does this. A
reviewer can look at the fixups separately first, then you fold them before merging.

To run the whole thing **without an editor** (in a script, or when you trust the
arrangement), replace the sequence editor with a command that accepts the list as it is:

```bash
GIT_SEQUENCE_EDITOR=true git rebase -i --autosquash main
```

`squash!` commits open the message editor during the rebase; `GIT_EDITOR=true` keeps the
combined message Git proposes.

## Splitting a commit

```bash
git rebase -i main           # mark the commit with "edit"
git reset HEAD~1             # Git stops after it; undo the commit, keep its changes
git add -p && git commit     # first part
git add -p && git commit     # second part
git rebase --continue
```

## Running a command after each commit

`git rebase -i --exec "make test" main` adds an `exec` line after every pick, so the rebase
stops at the first commit that does not pass. A branch where every commit builds keeps
`git bisect` useful (Phase 3).

## Cherry-pick

`git cherry-pick a1b2c3d` applies the change of one commit on top of the current branch as a
new commit. Typical use: backport a fix to a release branch.

```bash
git switch release/2.4
git cherry-pick -x a1b2c3d      # -x adds "(cherry picked from commit a1b2c3d)" to the message
git cherry-pick main~3..main    # a range
```

Prefer merging when you can: the same change in two commits on two branches can conflict
with itself later.

## Other rewriting tools

- `git commit --amend`: the last commit only.
- `git reset --soft main && git commit`: squash the whole branch into one commit.
- `git filter-repo` (a separate tool, recommended by the Git project over the old
  `filter-branch`): rewrite the **whole repository**, for example to remove a file from
  every commit (Phase 4).

## When not to rewrite

The golden rule still applies: do not rewrite commits that others build on. Rewriting your
feature branch before (and during) review is normal; rewriting `main` or a release branch is
not. Protected branches on the server enforce it.

## Summary

- `git rebase -i` edits a branch's history through a todo list: pick, reword, edit, squash,
  fixup, drop, reorder.
- `git commit --fixup=X` plus `git rebase -i --autosquash` folds review fixes into the
  commits they fix; `GIT_SEQUENCE_EDITOR=true` runs it without an editor.
- `--exec` checks every commit; `cherry-pick -x` copies a commit to another branch.
- Rewrite only your own branches; `--abort`, `ORIG_HEAD` and the reflog undo mistakes.

## Further reading

- Pro Git, "Rewriting History": <https://git-scm.com/book/en/v2/Git-Tools-Rewriting-History>
- git-rebase reference, "Interactive mode" and --autosquash: <https://git-scm.com/docs/git-rebase>
- git-commit reference, --fixup: <https://git-scm.com/docs/git-commit>
- git-cherry-pick reference: <https://git-scm.com/docs/git-cherry-pick>
- Atlassian, "Rewriting history": <https://www.atlassian.com/git/tutorials/rewriting-history>
- roadmap.sh topic "Rewriting History": <https://github.com/nilbuild/developer-roadmap/blob/master/roadmaps/git-github/content/rewriting-history@sOoC-XxEoIvwKct00oKlX.md>
