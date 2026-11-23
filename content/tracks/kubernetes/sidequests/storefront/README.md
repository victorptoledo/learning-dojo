# Sidequest: Storefront in production

The shop works: `web` (the pages), `api` (orders and payments) and `db` (PostgreSQL in a
StatefulSet) run in the namespace `shop`, and their manifests are in this folder. It is also
one compromised Pod away from a leaked database, one node drain away from an outage, and one
busy afternoon away from running out of Pods. You take it to production, in five stages:

1. a **network** that denies everything by default and allows each flow on purpose (the
   ingress controller to `web`, `web` to `api`, `api` to `db` and to the payment provider,
   everyone to DNS);
2. **availability**: disruption budgets that let drains proceed, Pods spread across zones and
   nodes, and a priority for what must keep running;
3. **autoscaling** for `web` and `api` that scales up fast and down slowly, and owns the
   replica counts;
4. **restricted Pods**: the Pod Security Standards `restricted` profile, read-only root file
   systems with small writable volumes, and one ServiceAccount per part that mounts no token;
5. **namespace guardrails**: Pod Security admission labels, a ResourceQuota sized for the
   autoscaled peak, default limits, and a least-privilege Role for the CI deployer.

How it is checked:

- Your work is the `.yaml` files at the top of this folder; add files as you need (for
  example `netpol.yaml`, `pdb.yaml`, `hpa.yaml`, `rbac.yaml`). Keep the names of the
  existing objects.
- There is no cluster. The dojo reads every `.yaml` file at the top of the folder, validates
  each object with kubeconform against the Kubernetes 1.36 schemas, and runs tests over the
  objects. NetworkPolicies are checked by simulating traffic between Pods, namespaces and
  outside addresses (who may reach whom, on which port), the way the API defines them; the
  quota by adding up requests at the HPA maximum; the Role by asking what it allows.
- Each stage keeps every earlier check.

The images are fictional (PostgreSQL is real), so on a cluster the shop would not start, but
`kubectl apply --dry-run=server -f .` on a local cluster with a NetworkPolicy-capable CNI
(Calico, Cilium) accepts every object. When you are done, `[A]` copies a review prompt: ask an
AI about your production setup.
