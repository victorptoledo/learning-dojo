The toolbox is used on Linux and macOS, on Intel and ARM. Extend `.github/workflows/ci.yml`:

**A matrix for `test`.**

- `strategy.matrix.os` is `[ubuntu-24.04, macos-15]`, plus one `include` entry,
  `{ os: ubuntu-24.04-arm, experimental: true }`, and the job `runs-on: ${{ matrix.os }}`;
- `fail-fast: false`: one failing runner must not cancel the others;
- the ARM runner is still experimental: its failures must not fail the workflow
  (`continue-on-error` is an expression on `matrix.experimental`);
- the tools differ by CPU now, so the cache key and the restore key of **both** jobs add
  `${{ runner.arch }}` after `${{ runner.os }}`:
  `toolbox-${{ runner.os }}-${{ runner.arch }}-${{ hashFiles('tools.lock') }}`.

**The test reports as artifacts.** After `scripts/ci.sh test`, each `test` job uploads
`reports/` with `actions/upload-artifact`, named `junit-${{ matrix.os }}` (one per runner,
since two artifacts cannot share a name), kept for at most 14 days (`retention-days`). It
runs even when the tests failed, which is when you need the report most, but not when the
run was cancelled: `if: ${{ !cancelled() }}`.

**One required check.** A branch protection rule names the checks a pull request needs;
with a matrix, those names change whenever the matrix does. Add a job `ci-ok` that needs
`lint` and `test`, always runs (`if: ${{ always() }}`; without it the job is skipped when a
job it needs fails, and a skipped check counts as passed), runs on `ubuntu-24.04` with a
timeout, and has one step with:

```yaml
        env:
          RESULTS: ${{ join(needs.*.result, ' ') }}
        run: |
          ...
```

The `run:` script reads `$RESULTS` (for example `success failure`) and exits `0` only when
every result is `success`; `failure`, `cancelled` and `skipped` all fail it. The tests run
your script with several values of `RESULTS`.
