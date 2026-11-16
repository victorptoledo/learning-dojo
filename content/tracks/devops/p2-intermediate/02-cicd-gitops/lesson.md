# Continuous Delivery and GitOps: Environments, Promotion and Argo CD

CI ends with an artefact: an image in a registry, tagged with its commit. **Continuous
delivery** takes it the rest of the way: through test environments, through approvals where
they are needed, into production, with a way back when something goes wrong. **GitOps** is the
most common way to do that on Kubernetes today.

## Environments and promotion

A typical path is `dev → staging → production`, sometimes with ephemeral preview environments
per pull request. Two rules make it trustworthy:

1. **One artefact**: the image is built once; every environment runs the same digest.
2. **Configuration per environment**: replicas, URLs, feature flags and secrets differ, and
   live next to the deployment description, not inside the image.

**Promotion** means "this exact version, tested in staging, now goes to production". It should
be a small, reviewable change (a new tag in the production configuration), not a rebuild.

## Push versus pull

| | Push (CI deploys) | Pull (GitOps) |
| --- | --- | --- |
| how | a pipeline job runs `kubectl apply`, `helm upgrade` or a cloud CLI | an agent in the cluster watches Git and applies what it finds |
| credentials | CI holds cluster or cloud credentials | the cluster pulls; CI needs no access to it |
| drift | invisible until the next deploy | detected and reported (or reverted) continuously |
| audit | pipeline logs | Git history: who changed what, reviewed by whom |
| rollback | re-run an old pipeline | `git revert` |

Push is simple and fits anything with an API (serverless, VMs, managed services). Pull fits
Kubernetes particularly well, because the cluster is itself a reconcile engine.

## GitOps

The OpenGitOps principles: the desired state is **declarative**, **versioned and immutable**
(in Git), **pulled automatically**, and **continuously reconciled**. In practice:

```text
app repo ── merge ──► CI: test, build, push ghcr.io/acme/orders:3f2c9e1
                          │ opens a PR (or commits) bumping the tag in
                          ▼
config repo:  envs/dev/orders/kustomization.yaml     images: [{ name: orders, newTag: 3f2c9e1 }]
              envs/prod/orders/kustomization.yaml    (promoted later by another PR)
                          │ watched by
                          ▼
Argo CD / Flux in each cluster ── sync ──► Deployments, Services, routes
```

An **Argo CD Application** points at a path in Git and a destination cluster and namespace:

```yaml
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata: { name: orders-prod, namespace: argocd }
spec:
  project: payments
  source:
    repoURL: https://github.com/acme/platform-config
    targetRevision: main
    path: envs/prod/orders
  destination:
    server: https://kubernetes.default.svc
    namespace: orders
  syncPolicy:
    automated:
      prune: true       # delete objects removed from Git
      selfHeal: true    # undo changes made by hand in the cluster
    syncOptions: [CreateNamespace=true]
```

The Application shows **Synced/OutOfSync** (Git versus cluster) and **Healthy/Degraded**
(how the objects are doing). Argo CD's **ApplicationSets** generate Applications per cluster
or per environment; **Flux** offers the same with `GitRepository` and `Kustomization` objects.
**Argo Rollouts** and **Flagger** add canary analysis on top (next lesson).

Image updates can be automated too (Argo CD Image Updater, Flux image automation, or a CI job
that opens a pull request with the new tag).

## Approvals and secrets

- An approval is a **pull request review** on the production configuration, enforced with
  branch protection and code owners, plus the CI environment protection rules for anything
  that still pushes.
- Secrets must not sit in Git in plain text. Options: **Sealed Secrets** (encrypted with the
  cluster's key, decryptable only there), **SOPS** with a cloud KMS, or the **External Secrets
  Operator**, which syncs from Vault or a cloud secrets manager.

## Rollbacks

With GitOps, rolling back means **reverting the commit** that changed the version; the
controller syncs the old state within a minute. A `kubectl rollout undo` is reverted by
self-heal, so it only helps if you pause the sync. Keep database migrations backwards
compatible (expand and contract), or a code rollback cannot undo them.

## Summary

- Build one artefact; promote the same version through environments with per-environment
  configuration.
- Push deployments are simple; pull-based GitOps adds drift detection, Git audit and keeps
  cluster credentials out of CI.
- Argo CD Applications sync a Git path to a cluster, with pruning and self-heal.
- Approvals are reviews in Git; secrets are encrypted or synced from a vault; rollback is a
  revert.

## Further reading

- OpenGitOps principles: <https://opengitops.dev/>
- Argo CD documentation: <https://argo-cd.readthedocs.io/en/stable/>
- Flux documentation: <https://fluxcd.io/flux/>
- External Secrets Operator: <https://external-secrets.io/>
- *Continuous Delivery*, Jez Humble and David Farley (Addison-Wesley, 2010), chapter 5 on the deployment pipeline
