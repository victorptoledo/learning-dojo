# Tags, Versions and Releases

A **tag** is a ref that names one commit and, unlike a branch, **never moves**. Tags mark
the commits you released, so you can always rebuild, diff or bisect from them.

## Two kinds of tags

| | Lightweight | Annotated |
| --- | --- | --- |
| What it is | a ref pointing straight at a commit | a ref pointing at a **tag object** that points at the commit |
| Stores | nothing but the hash | tagger, date, message, optional signature |
| Create | `git tag v1.4.0` | `git tag -a v1.4.0 -m "Release 1.4.0"` |
| Use for | private bookmarks | **releases** |

```bash
git tag                         # list
git tag -l "v1.*"               # filter
git show v1.4.0                 # tag message, then the commit
git tag -a v1.3.1 9fceb02 -m "Release 1.3.1"   # tag an older commit
git tag -s v1.4.0 -m "..."      # signed tag (Phase 4)
git tag -d v1.4.0-rc1           # delete locally
```

## Sharing tags

`git push` does **not** push tags by default.

```bash
git push origin v1.4.0                # one tag
git push --follow-tags                # commits plus the annotated tags that point at them
git push origin --delete v1.4.0-rc1   # delete on the server
git fetch --tags                      # get every tag
```

**Never move a published tag.** Other clones, CI caches, package registries and Go's
checksum database keep the old target; moving it means two people with "v1.4.0" have
different code. If a release is wrong, publish the next version.

## Semantic versioning

Most projects number releases `MAJOR.MINOR.PATCH` (semver):

| Bump | When | Example |
| --- | --- | --- |
| PATCH | backward-compatible bug fixes | 1.4.2 → 1.4.3 |
| MINOR | backward-compatible new features | 1.4.3 → 1.5.0 |
| MAJOR | anything that breaks users | 1.5.0 → 2.0.0 |

Pre-releases add a suffix: `2.0.0-rc.1`. The tag is usually the version with a `v` prefix.

## git describe

```text
$ git describe --tags
v1.4.0-12-g3f9a2c1
```

"12 commits after `v1.4.0`, at commit `3f9a2c1`" (the `g` stands for Git). Build scripts
embed it as the version of non-release builds; `--dirty` appends `-dirty` when the working
tree has changes. `git describe --abbrev=0` gives just the latest tag.

## Release notes from history

```bash
git log --oneline v1.4.0..v1.5.0             # everything in 1.5.0
git log --no-merges --format='- %s' v1.4.0..HEAD
git shortlog v1.4.0..v1.5.0                  # grouped by author
git log --oneline --grep='^feat' v1.4.0..HEAD
```

With **Conventional Commits** (`feat:`, `fix:`, `feat!:` or a `BREAKING CHANGE:` footer),
tools such as release-please or semantic-release compute the next version and write the
changelog from the subjects. Good commit messages become good release notes for free.

## Release branches and hotfixes

Teams that ship continuously tag `main` directly. Teams that maintain several versions cut a
**release branch**:

```text
main:        A - B - C - D - E - F
                      \
release/1.5:           C' - fix1 (v1.5.1) - fix2 (v1.5.2)
```

Fixes land on `main` first and are **cherry-picked** (`-x`) to the release branch, so `main`
never misses a fix that a release has.

## GitHub releases

A GitHub release is a page attached to a tag: notes, binaries, and generated release notes
from the merged pull requests. `gh release create v1.5.0 --generate-notes` creates the tag
(if needed) and the release in one step.

## Summary

- Tags name commits and never move; use annotated (or signed) tags for releases.
- Tags are pushed explicitly (`git push origin TAG`, `--follow-tags`); never move a pushed tag.
- Semver: patch for fixes, minor for compatible features, major for breaking changes.
- `git describe` versions any build; `git log vA..vB` and Conventional Commits produce notes.

## Further reading

- Pro Git, "Tagging": <https://git-scm.com/book/en/v2/Git-Basics-Tagging>
- git-tag reference: <https://git-scm.com/docs/git-tag>
- git-describe reference: <https://git-scm.com/docs/git-describe>
- Semantic Versioning 2.0.0: <https://semver.org/>
- Conventional Commits 1.0.0: <https://www.conventionalcommits.org/en/v1.0.0/>
- GitHub Docs, "About releases": <https://docs.github.com/en/repositories/releasing-projects-on-github/about-releases>
- roadmap.sh topic "Managing Tags": <https://github.com/nilbuild/developer-roadmap/blob/master/roadmaps/git-github/content/managing-tags@NeU38WPbEphJuJ_AMkH82.md>
