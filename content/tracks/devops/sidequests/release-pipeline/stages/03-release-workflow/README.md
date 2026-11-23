Releases now come from CI, never from a laptop. Write `.github/workflows/release.yml`, a
workflow named `release`:

| What | Rule |
| --- | --- |
| trigger | only pushes of tags matching `v*.*.*`: no branches, no pull requests |
| token | top-level `permissions: { contents: read }` |
| concurrency | one group per ref (`github.ref`), never cancelled in progress |
| shell | `defaults.run.shell: bash` |
| actions | pinned to a full commit SHA; every job has a `timeout-minutes` |

**Job `build`**, a matrix over `target: [linux-amd64, linux-arm64, darwin-arm64]` on
`ubuntu-24.04`. After `actions/checkout`, one step builds the target of the matrix:

- the tag name and the target reach the script through `env:` (`VERSION:
  ${{ github.ref_name }}`, `TARGET: ${{ matrix.target }}`), never by `${{ }}` in `run:`;
- `SOURCE_DATE_EPOCH` is the commit time, `git log -1 --format=%ct`, so a rebuild of the
  tag gives the same bytes;
- it runs `scripts/build.sh "$VERSION" "$TARGET"` (build.sh drops the `v`).

Then `actions/upload-artifact` keeps `dist/` as the artifact `dist-${{ matrix.target }}`,
failing when there is nothing to upload (`if-no-files-found: error`).

**Job `publish`** needs `build`, runs on `ubuntu-24.04` in the environment `release` (where
the repository can require an approval), and is the only job allowed to write: its
`permissions` are exactly `contents: write` (to create the release) and `actions: read` (to
download the run's artifacts). Its steps, in order:

1. `actions/checkout`;
2. collect: `gh run download "$GITHUB_RUN_ID" --repo "$GITHUB_REPOSITORY" --pattern
   'dist-*' --dir downloads` puts each artifact in its own folder (`downloads/dist-linux-amd64/`
   ...); move every archive into `dist/`;
3. `scripts/manifest.sh dist`, with `GIT_COMMIT: ${{ github.sha }}`;
4. `scripts/verify.sh dist`: a release that does not verify is never published;
5. `gh release create "$TAG" ...` with the archives, `SHA256SUMS` and `manifest.json`
   (given as `dist/...` paths), and
   `--verify-tag` (refuse to create the tag if it does not exist). A tag with a pre-release
   (`v1.3.0-rc.1`, any `-` in the name) also gets `--prerelease`. `gh` reads its token from
   `GH_TOKEN: ${{ github.token }}`.

**How the tests run it.** They run each matrix job's `run:` steps in a copy of your
repository with a fake `git` (the commit time is `1700000123`), keep its `dist/` the way
your upload step says, then run the `publish` steps with a fake `gh` that serves those
artifacts and records the release it is asked to create. They try a final tag, a
pre-release, and artifacts of two versions, which must stop the job before `gh release
create`.
