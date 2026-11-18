# Sidequest: Repository X-ray

You join a team and inherit a repository with years of history. Who wrote what? Who would
you ask about the billing module? Which files change all the time? Tools like `git-quick-stats`,
`git-fame` and code-forensics answer this from the history itself. You build `gitx.sh`, a
small cousin of them, in three stages:

1. `authors`: commits and lines added and removed per person, with `.mailmap` folding the
   many e-mails of one person into one name;
2. `owners`: who wrote the lines that are in a file or folder today (`git blame`), the share
   of each author and the bus factor;
3. `hotspots`: the files changed most in a period, counted under their current name even
   when they were renamed.

How it is checked:

- Your work is `gitx.sh` at the top of the folder (bash, started as `bash gitx.sh ...`). You
  may add more `.sh` files next to it; every script must pass ShellCheck.
- The tests build repositories in a temporary directory, with authors and dates they choose,
  run your script and compare what it prints and its exit status. The output formats in each
  stage's statement are the contract; fields are separated by one tab.
- Read history through Git's machine-friendly output (`git log --format`, `--numstat`, `-z`,
  `git blame --porcelain`, `git ls-tree`), never through the text meant for people. Your
  script must not change the repository: no commits, no checkout, no config.
- The tests run as an ordinary user in Debian 13 with Git 2.47, without network.

When you are done, `[A]` copies a review prompt: ask an AI about your script.
