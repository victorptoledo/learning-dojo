# Kubernetes in Production: Upgrades, Backups, GitOps and Policy

Getting a workload to run is the easy half. Production adds the questions that only matter
on a bad day: can we upgrade without an outage, restore after a disaster, see what is going
on, stop one team from hurting another, and keep the bill under control?

## The control plane: managed or yours

Managed services (EKS, GKE, AKS) run and back up the control plane, patch it and give you an
SLA; you own the nodes, add-ons and workloads. Self-managed clusters (kubeadm, k3s, Talos,
Rancher) give full control, and make etcd, certificates, upgrades and high availability your
job. Most teams should choose managed unless they have a clear reason.

A highly available control plane runs at least **three** API servers and an etcd cluster of
three or five members across failure domains; workloads spread across zones (Phase 2).

## Versions and upgrades

Kubernetes ships three minor releases a year, each supported for about **14 months**. Falling
behind is the most common production problem: old versions lose security fixes, and
upgrades get harder.

The **version skew policy** sets the order: the API server first, then controllers and
scheduler, then the kubelets, which may lag the API server by up to three minor versions
(and never be newer). Upgrade **one minor version at a time**:

1. Read the release notes and find **removed APIs** your manifests and charts still use
   (tools such as Pluto or kubent, or the API server's
   deprecation metrics).
2. Upgrade the control plane.
3. Upgrade nodes by replacing them: `kubectl cordon` and `kubectl drain` (which respects
   PodDisruptionBudgets), or a surge of new nodes in the node group.
4. Upgrade add-ons (CNI, CSI, CoreDNS, Ingress or Gateway controllers) to compatible versions.

Practise in a staging cluster with the same add-ons, and keep workloads tolerant of node
loss: replicas, spread, PDBs, readiness and graceful shutdown.

## Backups and disaster recovery

- **etcd** holds every object. Self-managed: take `etcdctl snapshot save` regularly, store it
  off the cluster, and **test restores**. Managed: the provider does it, but you cannot use
  it for your own restore of a namespace.
- **Cluster state as code**: with GitOps, Git can recreate every object, which is often the
  better recovery path.
- **Application data**: persistent volumes and databases need their own backups: volume
  snapshots, Velero (objects plus volumes), or the database's tooling. Restore drills prove
  the backups exist.

## GitOps and delivery

**GitOps** means the desired state of the cluster lives in Git, and an agent inside the
cluster (**Argo CD** or **Flux**) continuously applies it and reports drift. Changes go
through pull requests and review; nobody runs `kubectl apply` against production by hand;
rollback is a revert. CI builds and scans images, renders manifests (Helm, Kustomize),
validates them (kubeconform, policy tests) and updates the image tag in Git.

**Progressive delivery** reduces the blast radius of a bad release: canaries with traffic
weights (Gateway API, a service mesh), automatic analysis of error rates and latency, and
automatic rollback, with tools such as Argo Rollouts or Flagger. Feature flags decouple
deploying from releasing.

## Policy and multi-tenancy

- **RBAC** from your identity provider's groups; nobody uses cluster-admin day to day.
- **Pod Security Admission** at baseline everywhere and restricted where possible.
- **Policy engines** (Kyverno, OPA Gatekeeper, ValidatingAdmissionPolicy) for house rules:
  allowed registries, signed images, required labels and resources, no `latest`.
- **Namespaces per team or app**, with ResourceQuotas, LimitRanges and default-deny
  NetworkPolicies. Soft multi-tenancy (trusted teams) works in one cluster; hostile tenants
  need separate clusters or virtual clusters.
- Supply chain: pinned digests, image scanning, signatures verified at admission.

## Observability

- **Metrics**: Prometheus (or a managed equivalent) scraping the kubelet, kube-state-metrics,
  the control plane and your applications; dashboards and alerts on **SLOs**, not on every
  CPU spike.
- **Logs**: shipped from every node (Fluent Bit, Vector, the OpenTelemetry Collector) to a
  central store; structured JSON.
- **Traces**: OpenTelemetry instrumentation for request paths across services.
- **Events** and audit logs: who changed what, and what the cluster did about it.

## Cost

Requests decide what you pay for, because nodes are sized by them. Right-size requests from
measured usage (VPA recommendations), bin-pack with node autoscalers such as Karpenter, use
spot or preemptible nodes for interruption-tolerant work, scale idle workloads down (KEDA,
schedules), and show each team its share (OpenCost, Kubecost).

## A production readiness checklist

| Area | Ask |
| --- | --- |
| Workload | replicas ≥ 2, spread across zones, PDB, readiness, liveness only for the process, graceful SIGTERM |
| Resources | requests from measurements, memory limits, autoscaling where load varies |
| Security | non-root, restricted profile, own ServiceAccount, least-privilege RBAC, NetworkPolicies, secrets from a manager |
| Delivery | images by digest, manifests in Git, GitOps, rollback tested |
| Operations | dashboards and SLO alerts, centralised logs, runbooks, backup and restore tested |
| Cluster | supported version, upgrade plan, add-ons current, audit logging, policy engine |

## Summary

- Prefer managed control planes; run highly available across zones either way.
- Stay current: upgrade one minor version at a time, control plane first, nodes by draining.
- Back up etcd (or rely on GitOps) and application data, and test restores.
- GitOps makes Git the source of truth; progressive delivery limits bad releases.
- Policy, namespaces, quotas and NetworkPolicies make multi-tenancy safe; metrics, logs and
  traces make it visible; right-sized requests make it affordable.

## Further reading

- Version skew policy: <https://kubernetes.io/releases/version-skew-policy/>
- Upgrading a cluster: <https://kubernetes.io/docs/tasks/administer-cluster/cluster-upgrade/>
- Operating etcd (backups): <https://kubernetes.io/docs/tasks/administer-cluster/configure-upgrade-etcd/>
- Production environment: <https://kubernetes.io/docs/setup/production-environment/>
- Argo CD: <https://argo-cd.readthedocs.io/en/stable/>
- NSA/CISA Kubernetes Hardening Guide: <https://www.cisa.gov/news-events/alerts/2022/03/15/updated-kubernetes-hardening-guide>
