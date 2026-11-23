On-call wants to see the SLOs, not only be paged by them. `slogen.sh generate` now also
writes one Grafana dashboard per service, `OUTDIR/dashboards/SERVICE.json`, and prints
`slogen: wrote dashboards/SERVICE.json` for each. A dashboard file of a service that is no
longer in the spec is removed; other files stay.

**One more series.** The panels show each SLO over its own window, so the rules also
record `slo:sli_error:ratio_rate<window>` for the SLO's window (`30d`, `28d` or `7d`),
next to the seven of stage 1.

**The dashboard** (Grafana's JSON model):

| Field | Value |
| --- | --- |
| `uid` | `slo-SERVICE` |
| `title` | `SERVICE SLOs` |
| `tags` | `["slo", "generated"]` |
| `schemaVersion` | `39` |
| `editable` | `false`: edits belong in `slos.yaml` |
| `templating.list` | one variable, `{ "name": "datasource", "type": "datasource", "query": "prometheus" }` |
| `datasource` of every panel and target | `{ "type": "prometheus", "uid": "${datasource}" }` |

Three panels per SLO, in this order, on one row of the 24-column grid (`gridPos`), the rows
one under the other, never overlapping:

| `type` | `title` | targets (`expr`, all on the recorded series) |
| --- | --- | --- |
| `stat` | `SLO: SLI over WINDOW` | `A`: the SLI over the SLO's window, `1 -` its error ratio |
| `gauge` | `SLO: error budget left` | `A`: `1 -` the error ratio over the window divided by the budget |
| `timeseries` | `SLO: burn rate` | `A` (`legendFormat: "1h"`) and `B` (`"6h"`): the error ratio over 1h and 6h divided by the budget |

Panel `id`s are unique. Every query reads only series whose name starts with `slo:`: a
dashboard on raw metrics would run a 30-day `rate()` over every request each time someone
opens it.

The tests check the JSON, then turn every panel query into a recording rule and run it with
`promtool test rules` next to your rules: with 1% errors and a 98% objective, the SLI must
be 0.99, half the budget left, and a burn rate of 0.5. Regenerate and commit `rules/` and
`dashboards/`.
