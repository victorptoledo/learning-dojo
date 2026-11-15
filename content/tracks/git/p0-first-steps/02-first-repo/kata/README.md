# First Repository

Write `newrepo.sh DIR`. It turns `DIR` into a Git repository that is ready to use:

1. creates `DIR` if it does not exist (parents included);
2. initialises a repository whose branch is **`main`**, whatever `init.defaultBranch` says;
3. writes `README.md` containing one line, `# NAME`, where `NAME` is the last part of `DIR`
   (`projects/shop` gives `# shop`). If `DIR` already has a `README.md`, keep it unchanged;
4. writes `.gitignore` with two lines, `*.log` and `.env`;
5. commits **only** those two files with the message `Initial commit`;
6. prints `created DIR on branch main` (with `DIR` as given).

Other files already in `DIR` stay untracked. The commit uses the identity Git is configured
with: never run `git config` in the script (the tests set an identity for you).

```text
$ bash newrepo.sh projects/shop
created projects/shop on branch main
$ git -C projects/shop log --oneline
3f2a9c1 (HEAD -> main) Initial commit
```

Errors:

- no argument or more than one: print `usage: newrepo.sh DIR` to **stderr**, exit `2`;
- `DIR` is already a repository (it has a `.git`): print
  `newrepo.sh: DIR is already a git repository` to stderr and exit `1` without changing it.

Commands: `git init -b`, `git -C DIR ...`, `git add`, `git commit -m`, `basename`,
`mkdir -p`, `[ -e path ]`.

There are 11 tests: 1 in `tests/visible.bats` and 10 hidden ones, plus ShellCheck.
