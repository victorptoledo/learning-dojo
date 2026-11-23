Add **`run FILE`**: validate the spec (a bad one changes nothing in the cluster), then:

1. scale the canary Deployment to one replica: `kubectl -n NAMESPACE scale deployment
   DEPLOYMENT --replicas=1` (an earlier rollback may have left it at 0);
2. for each step, print `step I/N: weight W` and send `W`% of the traffic to the canary and
   `100 - W`% to stable;
3. then pause: `sleep INTERVAL` (the analysis interval, in seconds) as many times as the
   step has checks, so a pause of 2m with an interval of 1m is `sleep 60` twice. A step
   without a pause moves on at once;
4. after the last step, print `rollout: SERVICE promoted` and exit `0`.

**Moving the weights.** The HTTPRoute may have several rules; `k8s/route.yaml` has one for
testers, with a header, before the weighted one. Read the route (`kubectl -n NAMESPACE get
httproute ROUTE -o json`), find the **first rule whose `backendRefs` name both the stable
and the canary Service**, in whatever order they are listed, and patch only the two weights
of that rule, in one call:

```text
kubectl -n shop patch httproute checkout --type=json -p '[
  {"op": "add", "path": "/spec/rules/1/backendRefs/0/weight", "value": 90},
  {"op": "add", "path": "/spec/rules/1/backendRefs/1/weight", "value": 10}]'
```

(An HTTPRoute is a custom resource, so a strategic merge patch is not available: use a JSON
patch, or a JSON merge patch of the whole rule list.) Every `kubectl` call names the
namespace of the spec.

**Failures** print a line on stderr (after whatever `kubectl` said) and exit `1`: `rollout: cannot read httproute ROUTE`,
`rollout: cannot patch httproute ROUTE`, `rollout: cannot scale deployment DEPLOYMENT`,
and, when no rule has both backends, `rollout: httproute ROUTE has no rule with both STABLE
and CANARY` (before any patch).

The tests put a fake `kubectl`, `promq` and `sleep` first in `PATH`: always call them by
name, never by a full path. `sleep` must get whole seconds.
