The repository holds the tool (`src/greet.sh`, with the placeholders `@VERSION@` and
`@TARGET@`), its usage text (`share/greet/usage.txt`) and its `LICENSE`. Write
`scripts/build.sh`, which builds the release archives:

```text
$ SOURCE_DATE_EPOCH=1700000000 bash scripts/build.sh v1.2.0
dist/greet-1.2.0-linux-amd64.tar.gz
dist/greet-1.2.0-linux-arm64.tar.gz
dist/greet-1.2.0-darwin-arm64.tar.gz
```

**Arguments.** `build.sh VERSION [TARGET...]`, from any directory (the repository root is
the folder above `scripts/`).

- `VERSION` is a semantic version, `MAJOR.MINOR.PATCH` with an optional pre-release
  (`1.3.0-rc.1`); a leading `v` is dropped, so a tag name works. Anything else:
  `build.sh: invalid version: X` on stderr, exit `2`.
- The targets are `linux-amd64`, `linux-arm64` and `darwin-arm64`, built in the order given;
  with none, all three in that order. Another name: `build.sh: unknown target: X`, exit `2`.
- No argument at all prints `usage: build.sh VERSION [TARGET...]` on stderr and exits `2`.

**The archives.** `dist/` is emptied first, then each target gets
`dist/greet-VERSION-TARGET.tar.gz`, and its path is printed on one line. Inside, one top
folder, `greet-VERSION/`, with:

| Entry | Mode | Content |
| --- | --- | --- |
| `bin/greet` | `0755` | `src/greet.sh` with `@VERSION@` and `@TARGET@` replaced |
| `share/greet/usage.txt` | `0644` | as in the repository |
| `LICENSE` | `0644` | as in the repository |
| every folder | `0755` | |

```text
$ tar -xzf dist/greet-1.2.0-linux-arm64.tar.gz && greet-1.2.0/bin/greet --version
greet 1.2.0 (linux-arm64)
```

**Reproducible.** Building the same sources twice, on another machine, in another folder,
after another checkout, gives **the same bytes** (the same `sha256sum`). An archive records
more than file contents, so pin everything else:

- every timestamp is `SOURCE_DATE_EPOCH` (seconds since 1970, the standard variable for
  this; CI sets it to the commit time). When it is not set or not a number:
  `build.sh: SOURCE_DATE_EPOCH is not set` (or `... is not a number of seconds`), exit `2`;
- the modes come from the table, not from the checkout;
- entries are sorted, and owned by `0:0` with no user or group names;
- `gzip` stores neither a file name nor a time (`gzip -n`).

GNU tar has options for most of this: `--sort`, `--mtime`, `--owner`, `--group`,
`--numeric-owner`, `--format=gnu`.
