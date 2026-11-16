# Release Strategies: Rolling, Blue-Green, Canary and Feature Flags

Most outages are caused by changes. A release strategy decides how a new version reaches users
so that a bad one hurts as few of them as possible, for as short a time as possible.

## Deploy is not release

- **Deploy**: the new code is running somewhere in production.
- **Release**: users are exposed to the new behaviour.

Separating the two is the most powerful idea in this lesson. You can deploy on Tuesday, release
to 1% of users on Wednesday and to everyone on Thursday, and you can "unrelease" without
deploying anything, with a traffic weight or a feature flag.

## The strategies

| Strategy | How | Rollback | Cost and caveats |
| --- | --- | --- | --- |
| **Recreate** | stop all old instances, start new ones | redeploy the old version | downtime; only for dev or singletons |
| **Rolling** | replace instances a few at a time (Kubernetes' default) | roll back, which is another rolling update | both versions serve at once; a bad version reaches everyone gradually |
| **Blue-green** | run the full new version (green) next to the old (blue), test it, switch all traffic at once | switch back, instantly | double capacity during the switch; the switch is all or nothing |
| **Canary** | send a small share of real traffic (1%, 5%, 25%...) to the new version, compare, increase | set the weight to 0 | needs good metrics and traffic splitting; both versions must be compatible |
| **Shadow** (mirroring) | copy live traffic to the new version and discard its responses | nothing to undo | only for read-only paths; side effects are duplicated otherwise |

On Kubernetes, blue-green is two Deployments and a Service whose selector you switch
(`track: blue` → `track: green`); a canary is two Deployments and a route with weights:

```yaml
rules:
  - matches: [{ headers: [{ name: x-canary, value: always }] }]
    backendRefs: [{ name: checkout-canary, port: 80 }]
  - backendRefs:
      - { name: checkout-stable, port: 80, weight: 90 }
      - { name: checkout-canary, port: 80, weight: 10 }
```

Without a traffic-splitting layer, a canary by replica count (1 canary Pod next to 9 stable
ones behind one Service) gives roughly 10%, but you cannot go below one Pod's share.

## Deciding with metrics

A canary is only as good as the decision it feeds. Compare the canary with the stable
version, over the same period:

- **error rate** (5xx, failed jobs) and **latency** percentiles (p95, p99), the golden signals;
- **saturation** (CPU, memory, queue depth) for performance regressions;
- **business metrics**: sign-ups, payments completed, add-to-cart.

Automate it: **Argo Rollouts** (with `AnalysisTemplate`s querying Prometheus), **Flagger** or
a cloud deployment service steps the weight up on a schedule, runs the analysis at each step
and aborts (weight back to 0) when a metric crosses its threshold. Bake time matters: some
failures appear only after caches expire or a nightly job runs.

## Feature flags

A **feature flag** is a condition in the code (`if flags.enabled("new-checkout", user)`) whose
value is changed at run time from a flag service (LaunchDarkly, Unleash, Flagsmith, or the
vendor-neutral **OpenFeature** API). Flags let you:

- merge unfinished work to main (trunk-based development) with the flag off;
- release to internal users, then a percentage, then a region;
- turn a misbehaving feature off in seconds (a **kill switch**) without a deploy.

Flags are debt: each one doubles the paths to test. Give them owners and expiry dates, and
delete them when the release is complete.

## Database changes

Two versions of the code run at the same time during every strategy above, and a rollback runs
old code against the new schema. Schema changes must therefore be **backwards compatible**,
done with **expand and contract**:

1. *expand*: add the new column or table; the old code ignores it;
2. deploy code that writes both, backfill old rows, then reads the new one;
3. *contract*: in a later release, once no running version needs it, drop the old column.

Never rename or drop in the same release that stops using it.

## Summary

- Deploy puts code in production; release exposes users to it. Keep them separate.
- Rolling is the default; blue-green switches all at once with an instant way back; canaries
  expose a small share first and grow on evidence.
- Decide promotions from compared metrics (errors, latency, saturation, business), ideally
  automatically with Argo Rollouts or Flagger.
- Feature flags decouple release from deploy and give kill switches; remove them when done.
- Expand-and-contract migrations keep every step reversible.

## Further reading

- Martin Fowler, "BlueGreenDeployment": <https://martinfowler.com/bliki/BlueGreenDeployment.html>
- Danilo Sato, "CanaryRelease": <https://martinfowler.com/bliki/CanaryRelease.html>
- Pete Hodgson, "Feature Toggles": <https://martinfowler.com/articles/feature-toggles.html>
- Argo Rollouts: <https://argo-rollouts.readthedocs.io/en/stable/>
- Gateway API traffic splitting: <https://gateway-api.sigs.k8s.io/guides/traffic-splitting/>
