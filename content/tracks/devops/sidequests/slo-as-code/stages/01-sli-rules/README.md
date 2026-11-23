`slos.yaml` lists the services and their SLOs:

```yaml
services:
  - name: checkout                 # [a-z][a-z0-9-]*
    job: checkout                  # the Prometheus `job` label of its metrics
    runbook: https://runbooks.example.com/checkout
    slos:
      - name: availability         # [a-z][a-z0-9-]*
        objective: 99.9            # percent of good events, strictly between 0 and 100
        window: 30d                # 7d, 28d or 30d
        sli:
          type: availability       # bad events = responses with a 5xx `code`
          metric: http_requests_total
      - name: latency
        objective: 99
        window: 30d
        sli:
          type: latency            # bad events = requests slower than `threshold`
          metric: http_request_duration_seconds   # a histogram
          threshold: 0.3           # seconds, the `le` of one of its buckets
```

Write `scripts/slogen.sh generate SLOS OUTDIR`. It writes `OUTDIR/rules/slos.rules.yml`
(creating the folder), a Prometheus rule file with **one group per service**, named
`slo-SERVICE`, holding for each of its SLOs:

- the **error ratio** (bad events / all events) over each of the windows `5m`, `30m`, `1h`,
  `2h`, `6h`, `1d` and `3d`, recorded as `slo:sli_error:ratio_rate<window>`
  (`slo:sli_error:ratio_rate5m`, ...);
  - availability: the rate of the counter `METRIC` with a `code` matching `5..`, over the
    rate of all of `METRIC`, both for `job="JOB"` and summed over every other label;
  - latency: `1 -` the rate of `METRIC_bucket` with `le="THRESHOLD"` over the rate of
    `METRIC_count`, both for `job="JOB"`;
- the **error budget** `1 - objective / 100` as `slo:error_budget:ratio` (a constant:
  `vector(0.001)` for 99.9).

Every rule has exactly two labels, `service` and `slo`, so each series says which SLO it
belongs to. The first line of the file is a comment saying it is generated; then it prints
`slogen: wrote rules/slos.rules.yml (N rules)`. The same spec always gives the same file.

**A bad spec writes nothing.** `slogen.sh` checks the whole spec first and prints **every**
problem on stderr, one line each, as `slogen: SERVICE/SLO: problem` (or `slogen: SERVICE:
problem`, `slogen: no services`), then exits `1`. It checks the names, that each service
has a `job`, an `https://` runbook and at least one SLO, the objective, the window, the
`sli.type`, that there is a `metric`, and that a latency SLI has a positive `threshold`.
A missing file exits `1` too; a wrong command line prints a usage line on stderr and exits
`2`.

Finally, generate the rules of the repository's own `slos.yaml` and commit them:
`bash scripts/slogen.sh generate slos.yaml .`. The tests regenerate them and compare.
