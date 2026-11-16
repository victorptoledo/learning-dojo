# SLO Alerts: Multiwindow, Multi-Burn-Rate Rules

Turn an availability SLO into Prometheus rules: recording rules for the error ratio over
four windows, and two burn-rate alerts (page and ticket) that each need a long and a short
window to agree. Press **Enter** to create the kata folder and open it, edit
`slo.rules.yml` and save. The dojo checks the rules with `promtool` and runs hidden rule
unit tests that replay incidents, healthy traffic and a short spike.
