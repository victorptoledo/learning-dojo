# Sidequest: History surgeon

Someone committed a 300 MB video and an `.env` with production keys, three months and two
hundred commits ago. Deleting the files in a new commit does not help: every clone still
downloads them, and the key is one `git log -p` away. The fix is to rewrite history, carefully.
`git filter-repo` and BFG do it; you build `scrub.sh`, a small surgeon, in four stages:

1. `big` and `secrets`: find the big files and the leaked keys in every version of every file,
   including the ones deleted long ago;
2. `purge`: remove paths from every commit of every branch and tag, dropping commits that end
   up empty;
3. `redact`: replace leaked strings in every version of every file, changing only the objects
   that held them;
4. a backup before each rewrite, a map from old to new commits, `undo`, and `expire` to make
   the old objects really gone.

How it is checked:

- Your work is `scrub.sh` at the top of the folder (bash, started as
  `bash scrub.sh COMMAND REPO ...`). You may add more `.sh` files next to it; every script
  must pass ShellCheck.
- The tests build repositories in a temporary directory with branches, tags (annotated ones
  too), merges and dates they choose, run your script and then inspect the repository with
  plumbing: which objects exist, what each commit's tree holds, which commit ids changed.
- The rewriting stages are graded on what a real rewrite must keep: authors, dates and
  messages, merges, annotated tags, and the ids of every commit that did not need to change.
- `git filter-repo` is not installed. Build on `git fast-export` and `git fast-import` (or
  `git filter-branch`, slower and deprecated, if you must).
- The tests run as an ordinary user in Debian 13 with Git 2.47, without network.

> [!WARNING]
> Rewriting history changes commit ids: everyone with a clone must re-clone or rebase, and a
> leaked key must be **revoked first**. Rewriting only stops the leak from spreading.

When you are done, `[A]` copies a review prompt: ask an AI about your script.
