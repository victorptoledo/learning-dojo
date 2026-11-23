An SLO of 99.9% over 30 days allows 0.1% of bad events: the **error budget**. The **burn
rate** is how fast you spend it: at a burn rate of 1 the budget lasts exactly the window,
at 14.4 it is gone in two days. The Google SRE workbook alerts when a share of the budget
burns too fast, looking at a long window (is it serious?) **and** a short one (is it still
happening?). Add a second group per service, `slo-SERVICE-alerts`, with four alerts per
SLO, all named `SLOErrorBudgetBurn`:

| `severity` | `long_window` | short window | fires when this share of the budget burns in the long window | `for` |
| --- | --- | --- | --- | --- |
| `page` | `1h` | `5m` | 2% | `2m` |
| `page` | `6h` | `30m` | 5% | `15m` |
| `ticket` | `1d` | `2h` | 10% | `15m` |
| `ticket` | `3d` | `6h` | 10% | `15m` |

**Scaled to the SLO.** The burn-rate factor is `share × window / long window`: with a 30d
window, `0.02 × 720h / 1h = 14.4`, then 6, 3 and 1, the workbook's numbers. A 7d window
gives `3.36`, `1.4`, `0.7` and `0.2333` (round to four decimals). An alert fires when the
error ratio of **both** its long and its short window is above `factor × error budget`:

```yaml
      - alert: SLOErrorBudgetBurn
        expr: |
          slo:sli_error:ratio_rate1h{service="checkout",slo="availability"} > (14.4 * 0.001)
          and
          slo:sli_error:ratio_rate5m{service="checkout",slo="availability"} > (14.4 * 0.001)
        for: 2m
```

Build the alerts on the series of stage 1, never on the raw metrics. Each alert has:

- the labels `service`, `slo`, `severity` and `long_window`, no others;
- two annotations: `summary: "{{ $labels.service }} is burning the {{ $labels.slo }} error
  budget"` and `runbook_url`, the service's `runbook`.

Regenerate and commit `rules/slos.rules.yml`. `promtool check rules` refuses two rules of a
group with the same name and the same labels: the labels above keep them apart.

The tests feed your rules an hour of steady traffic at several error ratios, for SLOs of
99.9% and 99.5% over 30 and 7 days, then a burst of errors that stops: the pages must stop
with it (their short windows are clean again) while the tickets keep firing.
