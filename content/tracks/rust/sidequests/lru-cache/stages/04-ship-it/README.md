Ship the library the DevOps way. This stage is checked in the `dojo-devops` image (actionlint,
promtool and bats); the Rust tests of the earlier stages keep running in `dojo-rust`.

**1. `.github/workflows/ci.yml`**, a GitHub Actions workflow that:

- runs on pushes to `main` and on every pull request;
- gives the token read access only (`permissions: { contents: read }`);
- has a job `test` on `ubuntu-24.04` with a `timeout-minutes`;
- checks out the code first, then runs `cargo fmt --check`, `cargo clippy` with
  `-D warnings` and `cargo test` (the Rust toolchain is already on GitHub's runners).

**2. `alerts.rules.yml`**, a Prometheus rule file with one alert, `CacheHitRatioLow`, over the
metrics of stage 2 (prefix `cache`, so `cache_hits_total` and `cache_misses_total`, scraped
with a `job` label):

- it fires when the hit ratio of a job over the last 5 minutes, computed from the **rates** of
  the two counters, stays below 0.8 for 10 minutes;
- its only label is `severity: ticket` (plus the `job` of the series);
- its only annotation is `summary: "{{ $labels.job }} cache hit ratio below 80%"`.

```yaml
groups:
  - name: cache
    rules:
      - alert: CacheHitRatioLow
        expr: ...   # hits rate / (hits rate + misses rate) < 0.8, per job
        for: ...
        labels: { ... }
        annotations: { ... }
```

Hidden `promtool test rules` cases feed the alert a steady low ratio (it must fire), a
healthy cache (silent) and a short dip (silent, thanks to `for`).
