Everything so far depends on every future manifest being as careful as yours. Make the
namespace itself enforce the rules, cap what it can consume, and give the CI pipeline only
the access it needs.

**Pod Security admission.** The Namespace `shop` carries
`pod-security.kubernetes.io/enforce: restricted`, `enforce-version: v1.36`, and `audit` and
`warn` set to `restricted` as well. The API server then rejects any Pod that breaks the
standard of stage 4, whoever creates it.

**A ResourceQuota `shop-quota`** that the shop fits in at its busiest moment and nothing much
beyond:

- `hard` limits for `requests.cpu`, `requests.memory`, `limits.memory`, `pods`,
  `persistentvolumeclaims` and `requests.storage`, and `services.loadbalancers: "0"` and
  `services.nodeports: "0"` (the shop is reached through the Ingress only);
- **sized for the peak**: every workload at its HPA maximum (`db` at 1), plus one rolling
  update in flight (the default `maxSurge` of 25 %, rounded up, of those maximums) must fit in
  the CPU, memory and Pod limits, and the database's claim in the storage limits;
- and no more than **twice** that peak for CPU and memory requests: a quota ten times too
  big protects nothing.

**A LimitRange `shop-defaults`** for containers that forget their resources: `defaultRequest`
`cpu: 100m` and `memory: 128Mi`, `default` (the limit) `memory: 256Mi`, and `max` `memory: 4Gi`.

**A deployer for CI.** A ServiceAccount `deployer` (no automounted token: the pipeline gets a
short-lived one with `kubectl create token`), a **Role** (not a ClusterRole) `shop-deployer`
and a RoleBinding `shop-deployer` that gives it to `deployer`. The pipeline must be able to:

- `get`, `list`, `watch`, `create`, `update` and `patch` Deployments, StatefulSets,
  Services, ConfigMaps, HorizontalPodAutoscalers, PodDisruptionBudgets and NetworkPolicies;
- `get`, `list` and `watch` ReplicaSets, Pods and `pods/log` (for `kubectl rollout status`
  and to show the logs of a failed release).

And nothing else: no `delete`, no Secrets, no `pods/exec`, no RBAC objects, no changes to the
quota, the LimitRange or the Namespace, and no wildcards.

> [!TIP]
> Ask the API what a role allows: `kubectl auth can-i --list -n shop
> --as=system:serviceaccount:shop:deployer`, or one question at a time, `kubectl auth can-i get
> secrets -n shop --as=...`.
