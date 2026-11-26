apiVersion: backstage.io/v1alpha1
kind: Component
metadata:
  name: {{name}}
  annotations:
    dojo.dev/tier: {{tier}}
    dojo.dev/runbook: https://runbooks.example.com/{{name}}
spec:
  type: service
  lifecycle: production
  owner: group:{{team}}
