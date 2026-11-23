Now the environments. Each overlay starts from the base (`resources: [../../base]`) and says
only what is different. Nothing in `base/` changes for this stage.

| | `overlays/dev` | `overlays/prod` |
| --- | --- | --- |
| namespace of every object | `invoices-dev` | `invoices-prod` |
| image `ghcr.io/acme/invoices` | tag `3.1.0-rc.2` | tag `3.0.4` |
| replicas | 1 | 3 |
| `LOG_LEVEL` in `invoices-config` | `debug` | `warn` |
| the Secret `invoices-db` | generated, `password=dev-only-password` | **not** in the output: the platform's secret store creates it in the cluster |
| resources | the base's | requests `cpu: 500m`, `memory: 512Mi`; memory limit `1Gi` |
| label `environment` | `dev` | `prod` |

Use the Kustomize field made for each change instead of copying YAML:

- `namespace:`, `images:` (`newTag`) and `replicas:`;
- `configMapGenerator` with `behavior: merge` and the one key that changes: the other keys
  keep the base's values, and the merged ConfigMap gets a new hash;
- `secretGenerator` in dev only. A development password in Git is acceptable; a production
  one never is. In prod the container keeps referring to `invoices-db` by its plain name;
- a strategic merge patch (`patches:` with a file such as `resources.yaml`) for the prod
  resources;
- `labels:` with `includeSelectors: false` and `includeTemplates: true`: the environment
  label goes on every object and on the Pods, but never into the selectors, which cannot
  change after a Deployment is created.

The dojo builds `overlays/dev` and `overlays/prod`, validates them with kubeconform and
checks the table above. The base must still build as before.

> [!TIP]
> `diff <(kustomize build overlays/dev) <(kustomize build overlays/prod)` shows everything
> the two environments do differently. That diff is what a reviewer should read.
