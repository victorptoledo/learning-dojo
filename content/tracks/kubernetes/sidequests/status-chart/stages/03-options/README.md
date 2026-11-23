Different teams install the page differently: some share a ServiceAccount, some autoscale,
some need an extra variable. Add those switches, each with a default that works, and make
the page's settings a ConfigMap whose changes restart the Pods. Every new value goes into
`values.schema.json` too (the top level still allows only known keys).

New values and their defaults:

```yaml
serviceAccount:
  create: true
  name: ""           # empty: the fullname when created, "default" when not
autoscaling:
  enabled: false
  minReplicas: 2
  maxReplicas: 6
  targetCPUUtilizationPercentage: 75
config:
  STATUS_TITLE: Acme status
  REFRESH_SECONDS: 30
extraEnv: []         # more variables, as Kubernetes env entries
podAnnotations: {}
```

**ServiceAccount.** A helper `statuspage.serviceAccountName` gives `serviceAccount.name`, or,
when empty, the fullname if the chart creates the account and `default` if it does not.
With `create: true` the chart renders that ServiceAccount (with the chart labels). The
Deployment's Pods run as it, and neither the account nor the Pods mount an API token
(`automountServiceAccountToken: false`): the page never talks to the Kubernetes API.

**Settings.** A ConfigMap named by the fullname holds every key of `config`, each value a
string (`REFRESH_SECONDS: "30"`: use `quote`, since a ConfigMap value that is a number is
invalid), and the container loads it with `envFrom`. The Pod template carries the annotation
`checksum/config`: the SHA-256 of the rendered ConfigMap, so changing a setting changes the
Pod template and Helm rolls the Pods out. `podAnnotations` are added next to it.

**Autoscaling.** With `autoscaling.enabled` the chart renders an `autoscaling/v2`
HorizontalPodAutoscaler named by the fullname, for the Deployment, between `minReplicas` and
`maxReplicas`, at `targetCPUUtilizationPercentage` average CPU, and the Deployment has **no**
`replicas` field (otherwise every `helm upgrade` would reset what the HPA chose). A minimum
above the maximum fails with
`autoscaling.minReplicas must not be greater than autoscaling.maxReplicas`.

**Extra variables.** `extraEnv` entries are added as they are to the container's `env`
(`toYaml` and `nindent`), so `valueFrom` works too. In the schema, each entry is an object
with a `name`.

> [!TIP]
> The checksum idiom is `{{ include (print $.Template.BasePath "/configmap.yaml") . |
> sha256sum }}`. Try `helm template sp chart --set config.REFRESH_SECONDS=10 | grep checksum`
> and compare with the default.
