A sale doubles the traffic in minutes and is gone an hour later. Let `web` and `api` follow
it with HorizontalPodAutoscalers (`autoscaling/v2`, for example in `hpa.yaml`):

| HPA | Target | Replicas | Metrics (average utilization of the requests) |
| --- | --- | --- | --- |
| `web` | Deployment `web` | 3 to 12 | CPU 70 % |
| `api` | Deployment `api` | 3 to 10 | CPU 65 % and memory 80 % |

**Behaviour**, the same for both:

- **up fast**: `scaleUp` with `stabilizationWindowSeconds: 0`, two policies, `Percent` 100
  and `Pods` 4, each per 60 seconds, and `selectPolicy: Max` (double, or add four, whichever
  is more);
- **down slowly**: `scaleDown` with `stabilizationWindowSeconds: 300` (the highest
  recommendation of the last five minutes wins, so a short dip does not remove Pods) and one
  policy, `Percent` 20 per 60 seconds.

**The HPA owns the replica count.** Remove `replicas` from the `web` and `api` Deployments.
Kept, every `kubectl apply` (or GitOps sync) would reset the count to that number in the
middle of a peak. The budgets from stage 2 must still allow a drain at the HPA's minimum.

Utilization is a percentage of the **requests**, which is why every container has CPU and
memory requests: without them the HPA cannot compute anything.

> [!TIP]
> `kubectl get hpa -n shop -w` during a load test shows the targets and the replica counts
> moving; `kubectl describe hpa web -n shop` shows which policy limited each step.
