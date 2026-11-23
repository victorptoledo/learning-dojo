# Sidequest: Reproducible releases

`greet` is a tiny command-line tool. Today a release is whatever someone built on their
laptop and uploaded by hand: nobody can tell whether the archive on the release page is the
one that was tested, or rebuild it to check. You turn it into a release pipeline in four
stages:

1. `reproducible-build`: `scripts/build.sh` builds one archive per platform, and two builds
   of the same commit give the same bytes, wherever they run;
2. `checksums`: `SHA256SUMS`, a `manifest.json` that describes the release, and
   `scripts/verify.sh`, which refuses anything that does not match them;
3. `release-workflow`: a GitHub Actions workflow that a `v*` tag starts: a build matrix,
   the archives passed between jobs as artifacts, and a publish job that alone may write;
4. `promotion`: `scripts/promote.sh` and a workflow that move one verified release from
   staging to production by its digest, so production runs exactly what staging tested.

How it is checked:

- The tests copy your repository to a temporary folder and run your scripts for real:
  they build, unpack and compare archives, tamper with them and read what your scripts
  print, their exit status and the files they write.
- The workflows are linted with `actionlint` (with ShellCheck on every `run:` script) and
  read with `yq`. In stages 3 and 4 the tests also **run your workflow's `run:` steps**,
  in order, with the `${{ }}` expressions filled in and a fake `gh` and `git` first in
  `PATH`, so a step that would fail on GitHub fails here.
- Every `.sh` file must pass ShellCheck. The tests run in Debian 13 without network, with
  GNU `tar`, `gzip`, `sha256sum`, `jq` and `yq`.

Useful reading: reproducible-builds.org on `SOURCE_DATE_EPOCH` and archive metadata, and the
GitHub docs on artifacts, environments and `GITHUB_TOKEN` permissions. When you are done,
`[A]` copies a review prompt: ask an AI about your pipeline.
