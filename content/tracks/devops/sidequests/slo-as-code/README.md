# Sidequest: SLOs as code

Every team writes its SLO alerts by hand, copying a burn-rate rule from a blog post and
changing a number. Half of them are wrong for their objective, and nobody can tell which.
You make the SLOs **code**: one short `slos.yaml` per repository, and a generator,
`scripts/slogen.sh`, that writes everything else. In four stages:

1. `sli-rules`: Prometheus recording rules for the error ratio of every SLI over the seven
   windows the alerts need;
2. `burn-rate-alerts`: the multi-window, multi-burn-rate alerts of the Google SRE workbook,
   scaled to each SLO's own objective and window;
3. `dashboards`: one Grafana dashboard per service, built only on the recorded series;
4. `production-ready`: `slogen.sh lint` for any rule file, and the `prometheus.yml` that
   loads the generated rules.

How it is checked:

- `slogen.sh` is bash; it may use `yq` (mikefarah's, v4) and `jq`, as the tests do.
- The tests write their own `slos.yaml` files, run your generator into a temporary folder
  and then run **`promtool test rules`** on the output with synthetic traffic: so many
  requests, so many errors, so many slow ones. What your rules record and which alerts
  fire is the contract, not how the PromQL is spelled. (Compared values are rounded to
  six decimals, since `0.01` is never exactly `0.01` in floating point.)
- The generated files are committed next to `slos.yaml` (`rules/`, `dashboards/`), and
  the tests check that they are exactly what the generator writes today: in CI, a stale
  generated file is a failure.
- `promtool check rules` runs on `rules/*.yml`, ShellCheck on every `.sh` file. The tests
  run in Debian 13 without network.

Useful reading: the "Alerting on SLOs" chapter of the Google SRE workbook, and the
Prometheus docs on recording rules, `promtool test rules` and histograms. When you are
done, `[A]` copies a review prompt: ask an AI about your generator.
