# The Working Tree, the Index, Commits and Reading History

A change in Git lives in one of **three places**, and most commands move it from one to the
next.

```text
 working tree  --git add-->  index (staging area)  --git commit-->  repository
  your files                 the next commit, drafted               commits in .git
```

| Place | What it is | See it with |
| --- | --- | --- |
| **working tree** | the files on disk you edit | `git status`, `git diff` |
| **index** | a binary file (`.git/index`) listing the exact blobs of the **next** commit | `git diff --staged` |
| **repository** | the commits, starting from `HEAD` | `git log`, `git show` |

The index is what makes Git different from "save everything": you choose what goes into
the next snapshot. `git add` writes the file's blob and records it in the index;
`git commit` turns the index into a tree and a commit.

## Reading status

```text
$ git status --short
M  src/cart.py      # staged change (left column: index)
 M src/order.py     # unstaged change (right column: working tree)
MM README.md        # staged, then edited again
?? notes.txt        # untracked
A  src/tax.py       # new file, staged
```

`git diff` compares the working tree with the index; `git diff --staged` compares the index
with `HEAD`; `git diff HEAD` compares the working tree with `HEAD`. If a commit surprises
you, you skipped `git diff --staged`.

## Staging precisely

```bash
git add src/cart.py          # one file
git add -p src/checkout.py   # choose hunk by hunk: y, n, s (split), e (edit)
git add -u                   # every tracked file that changed, no new files
git add .                    # everything under this directory, new files included
git restore --staged FILE    # unstage, keep the change in the working tree
git rm --cached secrets.env  # stop tracking, keep the file on disk
git mv old.py new.py         # rename (Git detects renames by content anyway)
```

`git add -p` is how you split "fixed the bug and also tidied up" into two commits.

## Ignoring files

`.gitignore` lists patterns for **untracked** files Git should not offer to add:

```text
*.log          # any .log file, anywhere
/build/        # the build directory at the root only
node_modules/  # any directory with this name
!keep.log      # exception: track this one
```

Ignoring does not affect files that are already tracked: use `git rm --cached` first.
`git check-ignore -v FILE` tells you which rule matched. Personal patterns (your editor's
files) go in `~/.config/git/ignore` or `.git/info/exclude`, not in the shared `.gitignore`.

## Commits a reviewer can read

- **Atomic**: one logical change per commit. It builds, it passes the tests, and it can be
  reverted on its own.
- **Subject**: imperative, about 50 characters, no full stop: `Fix rounding of VAT on
  discounted items`. Read it as "If applied, this commit will ...".
- **Body** after a blank line, wrapped at about 72 columns: **why** the change was needed,
  what alternatives you rejected. The diff already says what.
- Many teams also follow **Conventional Commits** (`fix(cart): round VAT per line`), which
  tools use to build changelogs (Phase 3).

```bash
git commit                 # opens the editor: subject, blank line, body
git commit -m "Subject" -m "Body paragraph"
git commit --amend         # replace the last commit (only if not pushed yet)
```

## Reading history

```bash
git log --oneline --graph --decorate --all   # the DAG at a glance
git log -p -- src/cart.py                    # commits touching a path, with diffs
git log --author=ada --since="2 weeks ago"
git log -S "calculate_vat"                   # commits that add or remove this text ("pickaxe")
git log --grep="VAT" -i                      # search commit messages
git log main..feature                        # commits on feature not on main
git log --format='%h %an %ad %s' --date=short
git show a1b2c3d                             # one commit: metadata and diff
git show HEAD~2:src/cart.py                  # a file as it was two commits ago
git blame -L 40,60 src/cart.py               # who last changed each line, and in which commit
git log -L 40,60:src/cart.py                 # the full history of those lines
```

Revision syntax works everywhere: `HEAD~3` is three first-parents back, `HEAD^2` is the
second parent of a merge, `main@{yesterday}` is where `main` was yesterday (the reflog,
Phase 2). For scripts, `git rev-list --count HEAD` counts commits and `--format` gives you
a stable layout to parse.

## Summary

- Changes live in the working tree, the index and the repository; `add` and `commit` move
  them along, `diff`, `diff --staged` and `status` show where they are.
- `git add -p` makes atomic commits possible; `.gitignore` only affects untracked files.
- A good commit is one logical change with an imperative subject and a body that says why.
- `log` with filters, `show`, `blame` and `log -S` answer "when and why did this change?".

## Further reading

- Pro Git, "Recording Changes to the Repository": <https://git-scm.com/book/en/v2/Git-Basics-Recording-Changes-to-the-Repository>
- Pro Git, "Viewing the Commit History": <https://git-scm.com/book/en/v2/Git-Basics-Viewing-the-Commit-History>
- gitignore reference: <https://git-scm.com/docs/gitignore>
- gitrevisions, every way to name a commit: <https://git-scm.com/docs/gitrevisions>
- Chris Beams, "How to Write a Git Commit Message": <https://cbea.ms/git-commit/>
- Atlassian, "Saving changes": <https://www.atlassian.com/git/tutorials/saving-changes>
- roadmap.sh topic "Staging Area": <https://github.com/nilbuild/developer-roadmap/blob/master/roadmaps/git-github/content/staging-area@h71Tx3nkfUrnhaqcHlDkQ.md>
