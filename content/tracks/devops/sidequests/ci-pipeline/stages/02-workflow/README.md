Now CI runs `ci.sh` on every change. This stage adds `tools.lock` (the ShellCheck and bats
versions CI uses) and `scripts/install-tools.sh`, which installs them into
`~/.cache/toolbox` and prints the `bin` folder to add to `PATH`. It skips the download when
that folder already holds exactly the versions of `tools.lock`, so a cache makes it fast.

Write `.github/workflows/ci.yml`, a workflow named `ci`:

| What | Rule |
| --- | --- |
| triggers | pushes to `main` only, and every pull request |
| token | top-level `permissions: { contents: read }`, nothing more anywhere |
| concurrency | one group per ref (the group uses `github.ref`); a new run **cancels** the older one only for pull requests (`cancel-in-progress` is an expression on `github.event_name`), so runs on `main` always finish |
| shell | `defaults.run.shell: bash` |
| jobs | `lint` and `test`, both on `ubuntu-24.04` with `timeout-minutes` of at most 10; `test` needs `lint` |
| actions | pinned to a full 40-character commit SHA, with the version in a comment |

Each job runs these steps, in this order:

1. `actions/checkout`;
2. `actions/cache` for the path `~/.cache/toolbox`, with the key
   `toolbox-${{ runner.os }}-${{ hashFiles('tools.lock') }}` and the restore key
   `toolbox-${{ runner.os }}-` (an older cache is still a good start: the install script
   replaces what changed);
3. `scripts/install-tools.sh`, whose output goes to `$GITHUB_PATH` (for example
   `scripts/install-tools.sh >> "$GITHUB_PATH"`), so the next steps find the tools;
4. `scripts/ci.sh lint` in `lint`, `scripts/ci.sh test` in `test`.

```yaml
      - uses: actions/checkout@3d3c42e5aac5ba805825da76410c181273ba90b1 # v7.0.1
```

`actionlint` checks the workflow, and its ShellCheck pass covers every `run:` script. Do not
interpolate `${{ }}` event data into a `run:` script; pass it through `env:`.
