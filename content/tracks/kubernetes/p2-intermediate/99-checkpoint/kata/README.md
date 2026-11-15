# Checkpoint 2: A Production Overlay with Kustomize

The `catalog` service already has a Kustomize **base** (`base/`) and a **staging** overlay
(`overlays/staging/`). Write the **prod** overlay in `overlays/prod/`. The dojo renders both
overlays (`kustomize build overlays/staging` and `kustomize build overlays/prod`) and checks
what comes out.

| Rule (prod) | How |
| --- | --- |
| every object lives in the namespace `catalog-prod` | `namespace` |
| **4** replicas | `replicas` (or a patch) |
| image `ghcr.io/acme/catalog:2.3.1` | `images` |
| requests `cpu: 500m`, `memory: 512Mi`; a memory limit of `512Mi` | a patch |
| replicas spread over zones: `topology.kubernetes.io/zone`, `maxSkew: 1`, `DoNotSchedule` | a patch |
| `LOG_LEVEL=warn` in the generated `catalog-config` (keep the hash suffix) | `configMapGenerator`, `behavior: merge` |
| a PodDisruptionBudget `catalog` with `maxUnavailable: 1` for the catalog Pods | a new resource |
| an HTTPRoute `catalog` attached to the Gateway `public` in namespace `infra`, for host `catalog.example.com`, path prefix `/`, to the Service `catalog` on port 80 | a new resource |

Keep the base and the staging overlay working: staging must still render 1 replica in
`catalog-staging`. Probes, selectors and the Service from the base must survive the overlay.

```powershell
kubectl kustomize overlays/prod           # what the dojo checks
kubectl apply -k overlays/prod --dry-run=server   # on a cluster with the Gateway API CRDs
```

There are 13 tests: 1 in `tests/visible.bats`, 10 hidden ones and the kubeconform validation
of each overlay.
