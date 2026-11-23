Whoever downloads a release, a person or the next pipeline, needs to know that the archive
is the one you built. Add two scripts.

**`scripts/manifest.sh DIST`** describes the `greet-*.tar.gz` archives of the folder `DIST`.
It writes two files next to them:

- `SHA256SUMS`, in the `sha256sum` format, one line per archive sorted by file name, so
  `cd DIST && sha256sum -c SHA256SUMS` checks it;
- `manifest.json`:

```json
{
  "name": "greet",
  "version": "1.2.0",
  "commit": "4f2a9c1e...",
  "artifacts": [
    { "file": "greet-1.2.0-darwin-arm64.tar.gz", "target": "darwin-arm64", "size": 1532, "sha256": "9b1f..." }
  ]
}
```

`version` and `target` come from the file names (`greet-VERSION-TARGET.tar.gz`; a version
may contain dashes, a target is always `os-arch`); `commit` is the `GIT_COMMIT` environment
variable, or `"unknown"` without it; `size` is in bytes; `artifacts` are sorted by `file`.
Each file is written under a temporary name and renamed, so nobody reads half of it. It
prints `manifest: 1.2.0, 3 artifacts`.

It refuses, with a message on stderr and exit `1`, a folder with no archive and archives of
two versions; a missing folder or a wrong number of arguments exits `2`.

**`scripts/verify.sh DIST`** trusts nothing in `DIST` but itself. It exits `0` and prints
`verify: ok (3 artifacts, version 1.2.0)` only when:

| Problem (one line each, `verify: FILE: ...`) | Rule |
| --- | --- |
| `missing` | every artifact of the manifest exists |
| `size mismatch` | its size is the manifest's |
| `checksum mismatch` | its SHA-256 is the manifest's |
| `SHA256SUMS disagrees` | `SHA256SUMS` gives the same SHA-256 for it |
| `is not version VERSION inside` | its top folder is `greet-VERSION/`, the manifest's version (a renamed archive of another version fails) |
| `not in the manifest` | no other `.tar.gz` file is in `DIST`, and `SHA256SUMS` lists no other file |

It reports **every** problem it finds, one line each on stdout, then exits `1`. Without
`manifest.json` or `SHA256SUMS` it prints `verify: missing manifest.json` (or
`SHA256SUMS`) and exits `1`; a `manifest.json` that is not JSON with a `version` and an
`artifacts` array prints `verify: manifest.json is not a release manifest`, exit `1`.

`build.sh` from stage 1 stays as it is: the tests build with it, then describe and verify.
