# Horizontal Pod Autoscaler (HPA) and KEDA

Traffic is not flat. Provisioning for the yearly peak wastes money the rest of the time,
provisioning for the average fails at the peak. Kubernetes can change the number of
replicas with demand (**horizontal** scaling), the size of each Pod (**vertical**), and the
number of nodes underneath.

## The Horizontal Pod Autoscaler

An **HPA** watches metrics of a scalable workload (Deployment, StatefulSet, anything with a
`scale` subresource) and sets its replica count:

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: orders
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: orders
  minReplicas: 3
  maxReplicas: 20
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 70     # percent of the CPU **request**
```

Every 15 seconds the controller computes:

```text
desired = ceil( current replicas × current metric / target )
        = ceil( 4 × 140 % / 70 % ) = 8
```

With several metrics it takes the largest result. Then it clamps to min and max.

What this implies:

- **Utilization is relative to requests.** No CPU request, no CPU-based scaling: the HPA
  reports `<unknown>`. Wrong requests give wrong scaling.
- Resource metrics come from the **metrics server** (`kubectl top`). Custom metrics (requests
  per second from Prometheus) and external metrics (a queue in the cloud) need an adapter
  that serves the custom or external metrics API.
- The HPA owns `spec.replicas`. Remove `replicas` from the Deployment manifest you apply (or
  Helm chart), or every deploy resets the count and fights the autoscaler.

Metric types: `Resource` (CPU, memory of the whole Pod), `ContainerResource` (one
container, ignoring sidecars), `Pods` (a per-Pod custom metric), `Object` (a metric of
another object, such as an Ingress) and `External`.

Memory is a poor scaling signal for most runtimes: the JVM or a Go heap rarely shrinks, so
replicas go up and never down.

## Behaviour: avoiding flapping

```yaml
  behavior:
    scaleUp:
      stabilizationWindowSeconds: 0
      policies:
        - { type: Percent, value: 100, periodSeconds: 30 }   # at most double every 30 s
        - { type: Pods, value: 4, periodSeconds: 30 }        # or add 4 Pods
      selectPolicy: Max
    scaleDown:
      stabilizationWindowSeconds: 300      # use the highest recommendation of the last 5 min
      policies:
        - { type: Percent, value: 20, periodSeconds: 60 }
```

Scale up fast, scale down slowly: the default down window of five minutes stops a service
from shrinking between two bursts and then struggling to grow again. New Pods also need time
to become ready (JVM warm-up, cache fill): a readiness probe and some headroom in the
target (60-70 % rather than 90 %) keep latency acceptable during growth.

## KEDA: scaling on events

**KEDA** (Kubernetes Event-Driven Autoscaling) is a CNCF project that turns event sources
into metrics and manages an HPA for you. A `ScaledObject` names a workload and triggers:
Kafka consumer lag, RabbitMQ or SQS queue length, a Prometheus query, a cron schedule, and
dozens more.

```yaml
apiVersion: keda.sh/v1alpha1
kind: ScaledObject
metadata: { name: order-consumer }
spec:
  scaleTargetRef: { name: order-consumer }
  minReplicaCount: 0                 # scale to zero when idle
  maxReplicaCount: 24                # no more than the topic's partitions
  triggers:
    - type: kafka
      metadata:
        bootstrapServers: kafka.data:9092
        consumerGroup: orders
        topic: orders
        lagThreshold: "500"          # target lag per replica
```

Queue length is a **leading** signal: it grows before the CPU does, so KEDA reacts earlier
than a CPU-based HPA. It can also scale to zero, which plain HPAs cannot, at the cost of a
cold start for the first message. `ScaledJob` starts Jobs per batch of messages instead.

## Vertical and node autoscaling

- The **Vertical Pod Autoscaler** (VPA) recommends or sets requests from observed usage.
  Run it in recommendation mode to size requests; do not let VPA and an HPA act on the same
  CPU or memory metric. In-place Pod resizing (stable in recent versions) lets resources
  change without restarting the Pod.
- Node autoscalers add nodes when Pods are Pending for lack of capacity and remove
  underused ones: the **Cluster Autoscaler** works with node groups, **Karpenter** chooses
  instance types per Pending Pod. Both depend on correct **requests** and respect
  PodDisruptionBudgets when removing nodes.

The chain is: load grows → HPA or KEDA adds Pods → Pods are Pending → the node autoscaler
adds nodes → Pods are scheduled. Every link takes time; test it before the peak.

## Summary

- The HPA sets replicas to `ceil(current × metric / target)`, clamped to min and max.
- CPU utilisation is relative to requests; the Deployment must not set `replicas` itself.
- `behavior` scales up fast and down slowly to stop flapping.
- KEDA scales on event sources such as queue lag, earlier than CPU, and down to zero.
- VPA sizes requests; Cluster Autoscaler and Karpenter add the nodes the Pods need.

## Further reading

- Horizontal Pod Autoscaling: <https://kubernetes.io/docs/concepts/workloads/autoscaling/horizontal-pod-autoscale/>
- HPA walkthrough: <https://kubernetes.io/docs/tasks/run-application/horizontal-pod-autoscale-walkthrough/>
- Autoscaling workloads: <https://kubernetes.io/docs/concepts/workloads/autoscaling/>
- KEDA documentation: <https://keda.sh/docs/latest/>
- Karpenter: <https://karpenter.sh/docs/>
