`release.sh next REPO` prints the version the next release of `REPO` should have, from the
commits made on `HEAD` since the last release tag.

```text
$ git -C ~/src/shop log --format=%s v1.4.2..HEAD
docs: explain coupons
feat(cart): add coupon codes
fix: round VAT per line
$ bash release.sh next ~/src/shop
v1.5.0
```

**The last release** is the highest version among the tags reachable from `HEAD`
(`git tag --merged HEAD`) whose name is exactly `vMAJOR.MINOR.PATCH`: numbers without
leading zeros (`v1.10.0` is higher than `v1.9.3`). Other tags (`latest`, `v1.2`,
`v2.0.0-rc.1`, `version-3`) are not releases. The commits since are `TAG..HEAD`, without
merge commits.

**Each commit's subject** is read as `type(scope)!: description`: a lowercase type, an
optional scope in parentheses (`a-z`, `0-9`, `-`), an optional `!` and a colon followed by a
space. Subjects that do not have this form change nothing.

- a **breaking change** is a `!` before the colon, or a footer line in the body that starts
  with `BREAKING CHANGE: ` or `BREAKING-CHANGE: `;
- `feat` adds a feature; `fix` and `perf` fix something; other types (`docs`, `chore`,
  `refactor`, `test`, `ci`...) do not make a release on their own.

The strongest change decides, from the last release `X.Y.Z`:

| since the last release | `X.Y.Z` with `X ≥ 1` | `0.Y.Z` (still unstable) |
|---|---|---|
| a breaking change | `(X+1).0.0` | `0.(Y+1).0` |
| a `feat` | `X.(Y+1).0` | `0.Y.(Z+1)` |
| a `fix` or `perf` | `X.Y.(Z+1)` | `0.Y.(Z+1)` |

**No release tag yet:** the first release is `v0.1.0`, whatever the commits.

**Nothing to release** (no commits since the tag, or none that bumps): print
`release.sh: nothing to release since vX.Y.Z` on stderr and exit `1`. A repository without
commits prints `release.sh: no commits` the same way.

Errors, Unix style: no command, an unknown command or a wrong number of arguments prints
`usage: release.sh next|notes|tag REPO [OPTIONS]` on stderr and exits `2`; a `REPO` that
is not a Git repository prints `release.sh: not a git repository: REPO` and exits `2`.
