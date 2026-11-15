# Jobs, CronJobs and DaemonSets

Deployments keep a number of identical Pods running forever. Two common workloads do not fit
that shape: tasks that must **finish** (a migration, a report, a batch import), and agents
that must run **once on every node** (log collectors, monitoring agents, network plugins).

## Jobs

A **Job** runs Pods until a number of them **succeed**:

```yaml
apiVersion: batch/v1
kind: Job
metadata:
  name: import-orders
spec:
  completions: 10              # 10 successful Pods in total
  parallelism: 3               # at most 3 at a time
  completionMode: Indexed      # each Pod gets JOB_COMPLETION_INDEX 0..9 to pick its shard
  backoffLimit: 4              # give up after 4 failed retries
  activeDeadlineSeconds: 1800  # and after 30 minutes in any case
  ttlSecondsAfterFinished: 86400   # delete the Job and its Pods a day after it ends
  template:
    spec:
      restartPolicy: Never     # Never or OnFailure; Always is not allowed
      containers:
        - name: import
          image: ghcr.io/acme/importer:2.0.1
          args: ["--shard=$(JOB_COMPLETION_INDEX)"]
```

| Field | Meaning |
| --- | --- |
| `completions` / `parallelism` | how many successes are needed, how many Pods run at once |
| `backoffLimit` | failed attempts before the Job is marked `Failed` (retries back off exponentially) |
| `activeDeadlineSeconds` | a hard time limit for the whole Job |
| `ttlSecondsAfterFinished` | garbage-collect finished Jobs |
| `podFailurePolicy` | rules per exit code or condition: fail fast on a bug, ignore an eviction |
| `suspend` | create it paused, start it later |

`restartPolicy: OnFailure` restarts the container in the same Pod; `Never` creates a new Pod
for each attempt, which keeps the failed Pods (and their logs) for inspection. A Job's Pods
must be safe to run **more than once**: retries, node failures and evictions all cause
re-runs. Write idempotent tasks.

```powershell
kubectl create job manual-import --from=cronjob/nightly-import   # run a CronJob now
kubectl wait --for=condition=complete job/import-orders --timeout=30m
kubectl logs job/import-orders
```

## CronJobs

A **CronJob** creates a Job on a schedule:

```yaml
apiVersion: batch/v1
kind: CronJob
metadata:
  name: nightly-report
spec:
  schedule: "0 2 * * *"              # minute hour day month weekday
  timeZone: America/Sao_Paulo        # otherwise the controller's time zone, usually UTC
  concurrencyPolicy: Forbid          # Allow (default), Forbid or Replace
  startingDeadlineSeconds: 600       # a missed start may still run up to 10 minutes late
  successfulJobsHistoryLimit: 3
  failedJobsHistoryLimit: 5
  jobTemplate:
    spec:
      backoffLimit: 2
      activeDeadlineSeconds: 3600
      template:
        spec:
          restartPolicy: OnFailure
          containers:
            - name: report
              image: ghcr.io/acme/report:3.1.0
```

`concurrencyPolicy` decides what happens when the previous run is still going: `Allow` starts
another, `Forbid` skips the new one, `Replace` stops the old one. If the controller misses a
scheduled time (the cluster was down) it starts the run late only within
`startingDeadlineSeconds`. CronJobs promise "about once"; rare duplicate or missed runs are
possible, so idempotency matters here too.

## DaemonSets

A **DaemonSet** runs one Pod on every node (or on every node matching a selector), and adds
one automatically when a node joins:

```yaml
apiVersion: apps/v1
kind: DaemonSet
metadata:
  name: node-exporter
spec:
  selector: { matchLabels: { app: node-exporter } }
  updateStrategy:
    type: RollingUpdate
    rollingUpdate: { maxUnavailable: 1 }
  template:
    metadata: { labels: { app: node-exporter } }
    spec:
      tolerations:
        - operator: Exists              # also on tainted nodes, control plane included
      containers:
        - name: node-exporter
          image: quay.io/prometheus/node-exporter:v1.10.2
```

Typical DaemonSets: log shippers (Fluent Bit), metrics agents, CNI and CSI node plugins,
kube-proxy itself. They often need host access (host paths, the host network), which is why
they usually run in a dedicated, more privileged namespace (Phase 3). For "a few replicas
anywhere", use a Deployment instead.

## Which workload?

| Need | Workload |
| --- | --- |
| a stateless service, N replicas | Deployment |
| stable names and a volume per replica | StatefulSet |
| run to completion, once | Job |
| run to completion on a schedule | CronJob |
| one Pod per node | DaemonSet |

## Summary

- Jobs run Pods until enough succeed, with retries (`backoffLimit`), deadlines and cleanup.
- Indexed Jobs split work into shards; failed attempts are kept with `restartPolicy: Never`.
- CronJobs create Jobs on a schedule; set `timeZone`, `concurrencyPolicy` and history limits.
- DaemonSets place one Pod per node, including new nodes.
- Batch tasks must be idempotent: retries and duplicate runs happen.

## Further reading

- Jobs: <https://kubernetes.io/docs/concepts/workloads/controllers/job/>
- CronJob: <https://kubernetes.io/docs/concepts/workloads/controllers/cron-jobs/>
- DaemonSet: <https://kubernetes.io/docs/concepts/workloads/controllers/daemonset/>
- Automatic cleanup for finished Jobs: <https://kubernetes.io/docs/concepts/workloads/controllers/ttlafterfinished/>
