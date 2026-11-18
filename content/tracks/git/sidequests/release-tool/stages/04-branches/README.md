Version 1.3 is out, but a customer still runs 1.2 and needs a fix now. Teams keep a
**release branch** per minor version, `release/1.2`, cut from the `v1.2.0` tag: fixes land
there (often cherry-picked from `main`) and ship as `v1.2.1`, `v1.2.2`... while `main` moves
on. This stage teaches `release.sh` that workflow and keeps a `CHANGELOG.md` in the
repository.

**Release branches.** When `HEAD` is on a branch named exactly `release/X.Y` (numbers without
leading zeros), `next`, `notes` and `tag` work on that line only:

- the last release is the highest `vX.Y.Z` tag reachable from `HEAD`; tags of other lines
  (`v1.3.0` when you are on `release/1.2`) do not count. No such tag:
  `release.sh: release/X.Y has no vX.Y.Z tag`, exit `1`;
- only `fix` and `perf` commits may be released there: the next version is always
  `vX.Y.(Z+1)`. A `feat` or a breaking change since the last release is refused with
  `release.sh: release/X.Y only takes fixes: SUBJECT` (the subject of the oldest such commit),
  exit `1`;
- other commits (`docs`, `chore`...), "nothing to release" and `--pre` behave as on any
  branch.

Every other branch (`main`, `release/next`, `release/1.2.x`...) keeps the rules of the first
stages. The patch tags of a release branch are not reachable from `main` unless the branch
is merged back, so they do not change what `main` releases next.

**`tag --changelog`** also records the release in `CHANGELOG.md` at the top of the
repository, in a commit of its own, and tags that commit:

```text
$ bash release.sh tag ~/src/shop --changelog
committed CHANGELOG.md
tagged v1.5.0
```

- The file starts with the line `# Changelog` and an empty line; the new notes go right after
  them, followed by an empty line and the older entries. A missing file is created; a file
  whose first line is not `# Changelog` gets that line and an empty line added on top.
- The commit holds only `CHANGELOG.md` and its message is `chore(release): VERSION`; the tag
  points at it. Its notes (in the file and in the tag) are the ones `notes` printed before
  that commit.
- Options may come in any order: `tag REPO --pre rc --changelog`.
