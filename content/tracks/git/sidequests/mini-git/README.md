# Sidequest: Mini-git on plumbing

Git's porcelain (`add`, `commit`, `log`, `diff`) is built on a small set of plumbing commands
and a very simple storage format: blobs, trees, commits and tags, each named by the SHA-1 of
its content. Once you can read and write those objects yourself, nothing Git does is magic
any more. You build `minigit.sh`, which redoes some of Git's work from the objects up, in
five stages:

1. `hash`: the id of a file and of a whole folder, computed by hand with `sha1sum`;
2. `snapshot`: commit a folder to a branch without touching the index or the working tree;
3. `log` and `ls-tree`: walk commits and trees with `cat-file`;
4. `diff`: which files differ between two commits, without opening the subtrees that did not
   change;
5. `unreachable`: the objects, loose or packed, that nothing points at any more.

How it is checked:

- Your work is `minigit.sh` at the top of the folder (bash, started as
  `bash minigit.sh COMMAND ...`). You may add more `.sh` files next to it; every script must
  pass ShellCheck.
- **Only plumbing.** The tests put their own `git` first in `PATH`. It runs the real Git only
  for the plumbing commands each stage's statement allows (`cat-file`, `hash-object`,
  `mktree`...) and fails any other subcommand (`log`, `diff`, `add`, `ls-tree`, `rev-list`,
  `fsck`...) with exit status `99`. It also records every call, and what is sent to
  `cat-file --batch`, so a stage can check that you did not read what you did not need. Call
  `git`, never `/usr/bin/git`.
- The tests build repositories with the real Git and compare your output with what Git
  itself says (`hash-object`, `write-tree`, `log`, `ls-tree`, `diff`, `fsck`).
- The tests run as an ordinary user in Debian 13 with Git 2.47 (SHA-1 repositories), without
  network.

Reading: chapter 10 of *Pro Git*, "Git Internals", and `git help gitformat-index` /
`gitformat-pack` for the last stage. When you are done, `[A]` copies a review prompt: ask an
AI about your script.
