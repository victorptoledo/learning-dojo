# Packaging Manifests: Kustomize and Helm

One service, three environments, small differences: replicas, resources, image tags,
host names. Copying the manifests per environment works until the copies drift. Two tools
solve it differently: **Kustomize** patches plain YAML, **Helm** renders templates.

## Kustomize: bases and overlays

Kustomize is built into kubectl (`kubectl apply -k`, `kubectl kustomize`). A folder with a
`kustomization.yaml` lists resources and transformations. No templates: every input is
valid Kubernetes YAML.

```text
base/
  kustomization.yaml
  deployment.yaml
  service.yaml
overlays/
  dev/kustomization.yaml
  prod/
    kustomization.yaml
    replicas-and-resources.yaml
```

```yaml
# base/kustomization.yaml
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization
labels:
  - pairs: { app.kubernetes.io/name: orders }
    includeSelectors: true          # also into selectors and Pod template labels
resources: [deployment.yaml, service.yaml]
configMapGenerator:
  - name: orders-config
    literals: [LOG_LEVEL=info]
```

```yaml
# overlays/prod/kustomization.yaml
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization
namespace: orders-prod
resources: [../../base]
images:
  - name: ghcr.io/acme/orders
    newTag: 1.4.2
patches:
  - path: replicas-and-resources.yaml     # a strategic merge patch
  - target: { kind: Deployment, name: orders }
    patch: |-                             # or a JSON 6902 patch
      - op: replace
        path: /spec/template/spec/containers/0/resources/limits/memory
        value: 1Gi
configMapGenerator:
  - name: orders-config
    behavior: merge
    literals: [LOG_LEVEL=warn]
```

```yaml
# overlays/prod/replicas-and-resources.yaml: only what changes, with enough to identify it
apiVersion: apps/v1
kind: Deployment
metadata: { name: orders }
spec:
  replicas: 5
```

`kustomize build overlays/prod` prints the result; `kubectl apply -k overlays/prod` applies
it. Generators append a **content hash** to ConfigMap and Secret names and rewrite every
reference, so a config change becomes a rollout. Components share optional pieces between
overlays. The output is plain YAML you can diff and review.

## Helm: charts and releases

Helm is a **package manager**. A **chart** is a folder of Go templates plus default values:

```text
orders/
  Chart.yaml             # name, version (of the chart), appVersion
  values.yaml            # defaults
  templates/
    deployment.yaml
    service.yaml
    _helpers.tpl         # named templates
```

```yaml
# templates/deployment.yaml (excerpt)
spec:
  replicas: {{ .Values.replicaCount }}
  template:
    spec:
      containers:
        - name: {{ .Chart.Name }}
          image: "{{ .Values.image.repository }}:{{ .Values.image.tag | default .Chart.AppVersion }}"
          resources:
            {{- toYaml .Values.resources | nindent 12 }}
```

```powershell
helm template orders ./orders -f values-prod.yaml       # render only, like kustomize build
helm upgrade --install orders ./orders -n orders -f values-prod.yaml
helm history orders -n orders
helm rollback orders 3 -n orders
helm install cert-manager oci://quay.io/jetstack/charts/cert-manager --version v1.21.3 -n cert-manager --create-namespace
```

Each installation is a **release** with numbered revisions stored in the cluster (as
Secrets), so Helm can upgrade, list, roll back and uninstall everything it created. Charts
are versioned and published to OCI registries or chart repositories, and can depend on other
charts. `values.schema.json` validates values; hooks run Jobs before or after an install.

The cost: templates are text, not YAML-aware, so indentation bugs and unreadable
`{{- if }}` blocks are common. Keep charts boring, and always look at the rendered output.

## Choosing

| Situation | Tool |
| --- | --- |
| a handful of objects, one environment | plain manifests |
| your own services, several environments, reviewers who read YAML | Kustomize |
| a package other people install and configure (or third-party software) | Helm |
| both | Kustomize for your apps, Helm charts for dependencies (Kustomize can inflate charts with `helmCharts`) |

GitOps tools (Argo CD, Flux) understand both and apply the rendered output from Git (Phase 4).
Whatever you choose, render in CI and validate the result (kubeconform, policies): that is
exactly what this dojo's checker does.

## Summary

- Kustomize: a base plus overlays with patches, `images`, `namespace`, labels and generators.
- Generators add a content hash so config changes roll out.
- Helm: charts of templates and values, installed as releases with history and rollback.
- Use Kustomize for your own multi-environment YAML, Helm for packages and dependencies.
- Always render and validate before applying.

## Further reading

- Kustomize documentation: <https://kubectl.docs.kubernetes.io/references/kustomize/>
- Declarative management with Kustomize: <https://kubernetes.io/docs/tasks/manage-kubernetes-objects/kustomization/>
- Helm documentation: <https://helm.sh/docs/>
- Helm chart template guide: <https://helm.sh/docs/chart_template_guide/>
- Helm chart best practices: <https://helm.sh/docs/chart_best_practices/>
