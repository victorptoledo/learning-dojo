A chart is an interface, and `values.yaml` is its input. Today `--set replicacount=3` (a typo)
is silently ignored, and `--set replicaCount=0` or `--set service.port=http` produce objects
the cluster rejects at install time, or worse, accepts. Make bad values fail at
`helm template`, with a message that says what is wrong.

**`chart/values.schema.json`** (JSON Schema; Helm checks the merged values against it before
rendering anything):

- the top level allows only the keys the chart knows (`additionalProperties: false`), so a
  typo is an error; `image`, `service`, `ingress` and `ingress.tls` do the same;
- `replicaCount`: an integer, at least 1;
- `image.repository`: a non-empty string; `image.tag`: a string; `image.pullPolicy`: one of
  `Always`, `IfNotPresent`, `Never`;
- `service.type`: one of `ClusterIP`, `NodePort`, `LoadBalancer`; `service.port`: an integer
  from 1 to 65535;
- `resources`: an object; `nameOverride` and `fullnameOverride`: strings;
- the `ingress` values below, with their types.

**An optional Ingress.** New values, off by default:

```yaml
ingress:
  enabled: false
  className: nginx
  host: ""
  tls:
    enabled: false
    secretName: ""
```

`templates/ingress.yaml` renders nothing unless `ingress.enabled` is true. Then it renders an
Ingress named by the fullname helper, with the chart labels, the class, one rule for the host
whose path `/` (`Prefix`) goes to the release's Service on the port named `http`, and, when
`ingress.tls.enabled` is true, TLS for that host with that Secret.

A host is not optional for an enabled Ingress, and a TLS entry is not optional without its
Secret. Use `required` so the render fails with exactly these messages:

- `ingress.host is required when ingress.enabled is true`
- `ingress.tls.secretName is required when ingress.tls.enabled is true`

> [!TIP]
> The schema runs on `helm install`, `upgrade`, `template` and `lint`, for values from
> files and from `--set` alike. Messages from `required` and `fail` are the chart talking to
> the person installing it: say which value to set.
