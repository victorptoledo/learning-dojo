# TODO: SLO rules for every service (see README.md). Today every service gets the same
# cause-based alert, whatever its tier.
groups:
  - name: {{name}}-alerts
    rules:
      - alert: HighErrorRate
        expr: |
          sum by (job) (rate(http_requests_total{job="{{name}}", code=~"5.."}[5m])) > 1
        for: 1m
        labels:
          severity: page
        annotations:
          summary: {{name}} returns errors
