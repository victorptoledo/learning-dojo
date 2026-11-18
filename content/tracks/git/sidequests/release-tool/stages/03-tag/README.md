`release.sh tag REPO [--pre ID]` makes the release: an **annotated tag** on `HEAD`, named
with the next version, whose message is the release notes. It prints `tagged VERSION`.

```text
$ bash release.sh tag ~/src/shop
tagged v2.0.0
$ git -C ~/src/shop tag -l --format='%(contents)' v2.0.0
## v2.0.0 (2024-03-09)

### ⚠ BREAKING CHANGES
...
```

- The tag message is exactly what `notes` prints for that version. The headings start with
  `#`, which `git tag` treats as comment lines and strips by default: keep them
  (`--cleanup=verbatim`).
- The tag is annotated (a tag object with tagger and date, `git cat-file -t` says `tag`), not
  a lightweight one. It points at the commit at `HEAD`. Nothing else changes: no commit, no
  branch moves, nothing is pushed.

**Refuse to release** (print the message on stderr, exit `1`, create nothing), checking in
this order:

1. `HEAD` is not on a branch: `release.sh: HEAD is detached`;
2. tracked files have changes, staged or not: `release.sh: uncommitted changes` (untracked
   files are fine: they are not part of the release);
3. nothing to release or no commits: the messages of `next`;
4. the tag already exists, even on another branch: `release.sh: tag VERSION already exists`.

**Release candidates.** `--pre ID` (`ID` is lowercase letters: `rc`, `beta`...) tags a
pre-release of the next version instead: `VERSION-ID.N`, where `N` is one more than the
highest `VERSION-ID.N` tag that exists (in the whole repository), starting at `1`. The notes'
title shows that name: `## v2.0.0-rc.2 (2024-03-12)`. Pre-release tags are not releases
(stage 1), so after `v2.0.0-rc.1` the next version is still `v2.0.0`, until `v2.0.0` itself is
tagged.

A missing or invalid `ID` or any other option is the usage error (`2`).
