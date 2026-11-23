`chart/` holds `Chart.yaml` (chart `statuspage` 0.1.0, `appVersion: 2.3.0`) and the team's
Deployment and Service, with every name and value written in. Make them templates.

**Helpers** in `templates/_helpers.tpl` (`define`, then `include` in the templates):

| Helper | Gives |
| --- | --- |
| `statuspage.name` | the chart name, or `nameOverride` |
| `statuspage.fullname` | `fullnameOverride` if set; else the release name if it already contains the chart name; else `<release>-<chart name>`. Always cut to 63 characters with no trailing `-` |
| `statuspage.selectorLabels` | `app.kubernetes.io/name` (the name helper) and `app.kubernetes.io/instance` (the release name) |
| `statuspage.labels` | the selector labels plus `helm.sh/chart: statuspage-0.1.0`, `app.kubernetes.io/version` (the app version) and `app.kubernetes.io/managed-by` (`.Release.Service`) |

So `helm template sp chart` names everything `sp-statuspage`, and `helm template statuspage
chart` names everything `statuspage` (not `statuspage-statuspage`).

**Templates.** The Deployment and the Service are named by the fullname helper and labelled
by the labels helper; the Deployment's selector, its Pod labels and the Service's selector use
the selector labels (so two releases in one namespace never select each other's Pods).

**Values** in `values.yaml`, with these defaults:

```yaml
replicaCount: 2
image:
  repository: ghcr.io/acme/statuspage
  tag: ""            # empty: the chart's appVersion
  pullPolicy: IfNotPresent
service:
  type: ClusterIP
  port: 80
resources:
  requests: { cpu: 50m, memory: 64Mi }
  limits: { memory: 128Mi }
nameOverride: ""
fullnameOverride: ""
```

The container `statuspage` runs `<repository>:<tag or appVersion>` with that pull policy, port
`8080` named `http`, readiness `GET /ready` and liveness `GET /health` on `http`, and the
`resources` block as it is in the values (`toYaml` and `nindent`). The Service has the type
and port from the values and targets `http`.

> [!TIP]
> `helm create demo` writes a chart with these exact helpers. Read its `_helpers.tpl`, but
> write yours: knowing why `trunc 63 | trimSuffix "-"` is there is the point.
