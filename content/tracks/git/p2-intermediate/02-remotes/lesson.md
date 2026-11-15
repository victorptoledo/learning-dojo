# Remotes: clone, fetch, pull and push

A **remote** is a name for another repository's URL. Collaboration in Git is nothing more
than copying commits and moving refs between repositories.

```bash
git clone git@github.com:acme/shop.git   # copy everything, add remote "origin", check out main
git remote -v                            # origin  git@github.com:acme/shop.git (fetch/push)
git remote add upstream https://github.com/original/shop.git
git remote set-url origin git@github.com:me/shop.git
git remote rename upstream source
```

URLs can be SSH (`git@host:owner/repo.git`, authenticated with an SSH key), HTTPS
(authenticated with a token through a credential helper) or a plain path (`/srv/git/shop.git`,
handy for tests).

## Remote-tracking branches

After a fetch, Git stores what it saw on the remote in **remote-tracking branches**:
`origin/main`, `origin/feature/search`. They are local refs (`refs/remotes/origin/...`) that
**only fetch moves**. You never commit on them; they are your last snapshot of the server.

```text
               your commits
   A - B - C - X - Y          main
            \
             D - E            origin/main  (what the server had at your last fetch)
```

An **upstream** links a local branch to a remote-tracking branch, so `git status`,
`git pull` and `git push` know where to look:

```text
$ git status
On branch main
Your branch and 'origin/main' have diverged,
and have 2 and 2 different commits each, respectively.
```

```bash
git branch -vv                       # every branch with its upstream and ahead/behind
git push -u origin feature/search    # push and set the upstream in one go
git branch --set-upstream-to=origin/main
```

## fetch, pull, push

| Command | Talks to the server | Changes your branches |
| --- | --- | --- |
| `git fetch` | downloads new commits, moves `origin/*` | **no** |
| `git pull` | fetch, then merge (or rebase) the upstream into the current branch | yes |
| `git push` | uploads commits, asks the server to move its branch | the server's |

`fetch` is always safe; run it often and look before you integrate:
`git log --oneline main..origin/main` shows what is new.

`git push` only succeeds if it is a **fast-forward** for the server's branch. If someone
pushed first you get `rejected (fetch first)`: fetch, integrate (rebase or merge), push again.

## Force-pushing safely

After rebasing or amending a branch you already pushed, its new tip is not a descendant of
the old one, so a normal push is rejected. You must force it:

```bash
git push --force-with-lease          # only if the server still has what I last fetched
git push --force                     # overwrite whatever is there (dangerous)
```

`--force-with-lease` compares the server's branch with your `origin/<branch>`. If a
colleague pushed in the meantime, the lease fails and nothing is lost. Plain `--force` would
silently throw their commits away. Protect shared branches on the server so nobody can
force-push them at all (Phase 4).

## Housekeeping

```bash
git fetch --prune              # delete origin/* refs whose branches were deleted on the server
git config --global fetch.prune true
git push origin --delete feature/search   # delete a branch on the server
git ls-remote origin           # what the server has, without fetching
git remote show origin         # upstreams, stale branches, what push would do
```

## Forks

On GitHub you usually cannot push to an open-source project, so you **fork** it (a server-side
copy under your account), clone the fork as `origin`, and add the original as `upstream`:

```bash
git fetch upstream
git rebase upstream/main        # bring your branch up to date
git push --force-with-lease origin fix/typo   # update the pull request
```

The pull request goes from `me/shop:fix/typo` to `original/shop:main`.

## Refspecs, briefly

Behind every fetch and push there is a **refspec**: `+refs/heads/*:refs/remotes/origin/*`
means "copy every server branch into `origin/*`, even when it is not a fast-forward (`+`)".
`git push origin HEAD:refs/heads/release` pushes the current commit to a branch named
`release`. You rarely write them by hand, but they explain what fetch and push move.

## Summary

- A remote is a named URL; `origin/main` is your local, read-only snapshot of the server's
  `main`, moved only by fetch.
- `fetch` downloads and is always safe; `pull` is fetch plus merge or rebase; `push` must
  be a fast-forward on the server.
- After rewriting a pushed branch, use `--force-with-lease`, never plain `--force`.
- Forks use two remotes: `origin` (yours) and `upstream` (the original).

## Further reading

- Pro Git, "Working with Remotes": <https://git-scm.com/book/en/v2/Git-Basics-Working-with-Remotes>
- Pro Git, "Remote Branches": <https://git-scm.com/book/en/v2/Git-Branching-Remote-Branches>
- Pro Git, "The Refspec": <https://git-scm.com/book/en/v2/Git-Internals-The-Refspec>
- git-push reference (see --force-with-lease): <https://git-scm.com/docs/git-push>
- Atlassian, "Syncing": <https://www.atlassian.com/git/tutorials/syncing>
- GitHub Docs, "Fork a repository": <https://docs.github.com/en/pull-requests/how-tos/work-with-forks/fork-a-repo>
- roadmap.sh topic "Fetch without Merge": <https://github.com/nilbuild/developer-roadmap/blob/master/roadmaps/git-github/content/fetch-without-merge@Gybnekbd1rJKTOf4hJ4Sh.md>
