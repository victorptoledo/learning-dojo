# Sidequest: Release from commits

When every commit follows [Conventional Commits](https://www.conventionalcommits.org/)
(`feat(cart): add coupons`, `fix!: ...`), a release needs no meeting: the history says which
version comes next and what goes in the notes. Tools like semantic-release, release-please
and git-cliff do it. You build `release.sh`, a small one, in four stages:

1. `next`: the next semantic version since the last release tag;
2. `notes`: the release notes in Markdown, grouped by type, breaking changes first;
3. `tag`: an annotated tag holding the notes, refused when the repository is not in a state
   to release, and release candidates (`v2.0.0-rc.1`);
4. `branches`: patch releases on `release/X.Y` branches and a `CHANGELOG.md` committed with
   the release.

How it is checked:

- Your work is `release.sh` at the top of the folder (bash, started as
  `bash release.sh COMMAND REPO ...`). You may add more `.sh` files next to it; every script
  must pass ShellCheck.
- The tests build repositories in a temporary directory with the commits, tags, branches and
  dates they choose, run your script and compare what it prints, its exit status, and the
  tags, commits and files it leaves. The formats in each stage's statement are the contract.
- Read history through `git log --format`, `git tag --list` and `git rev-parse`, never the
  text meant for people. Only `tag` changes the repository, and only as its statement says.
- The tests run as an ordinary user in Debian 13 with Git 2.47, without network.

When you are done, `[A]` copies a review prompt: ask an AI about your script.
