# Sidequest: A Helm chart for the status page

Every team at Acme wants its own copy of the status page (`ghcr.io/acme/statuspage`, listening
on 8080 with `GET /ready` and `GET /health`), and each copy is a hand-edited set of YAML
files. You turn it into one Helm chart that anyone can install with their own values, in four
stages:

1. **templates** with helpers for names and labels, so two releases never collide and the
   image tag follows the chart's `appVersion`;
2. **validated values**: a `values.schema.json` that rejects typos and bad types before
   anything reaches the cluster, and an Ingress that refuses to render without a host;
3. **optional pieces** switched by values (ServiceAccount, autoscaling, extra variables) and
   a ConfigMap whose changes restart the Pods;
4. the **release lifecycle**: a database migration hook, a `helm test`, release notes and
   `helm lint --strict`.

How it is checked:

- Your work is the chart in `chart/` (`Chart.yaml`, `values.yaml`, `values.schema.json`,
  `templates/`). Helm 4.3 is the tool.
- There is no cluster. The dojo runs `helm template` with the chart's defaults and with the
  values each stage names (`--set`, `-f`), validates the output with kubeconform against the
  Kubernetes 1.36 schemas, and runs tests over the rendered objects; values that must be
  rejected must make `helm template` fail. Each stage keeps every earlier check.
- Try it yourself: `helm template sp chart`, `helm template sp chart --set ingress.enabled=true
  --set ingress.host=status.acme.test`, and `helm install sp chart --dry-run=client` to see
  the release notes.

When you are done, `[A]` copies a review prompt: ask an AI about your chart.
