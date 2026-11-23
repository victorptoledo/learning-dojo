A release replaces every Pod. Make sure nobody notices: new Pods are proven before old ones
go, a Pod that is stopping finishes what it is doing, and the API's settings live outside
the image.

**Rollouts**, on both Deployments:

- `RollingUpdate` with `maxSurge: 1` and `maxUnavailable: 0`: a new Pod must be ready before
  an old one is removed, so capacity never drops below 2.
- `minReadySeconds: 10`: a Pod must stay ready for 10 seconds before it counts as available
  (a Pod that crashes right after its first good probe stops the rollout).
- `progressDeadlineSeconds: 300` and `revisionHistoryLimit: 5` (five old ReplicaSets to roll
  back to with `kubectl rollout undo`).
- Images keep an exact tag (never `latest`).

**Shutdowns.** When a Pod is deleted, removing it from the Service and sending `SIGTERM`
happen at the same time, so for a moment new requests still arrive at a process that is
stopping.

- Both containers wait first: `lifecycle.preStop.sleep.seconds: 5`.
- The grace period covers that wait plus the time to finish open requests: the API needs
  30 seconds, the web part 10. `terminationGracePeriodSeconds` is at least the preStop
  sleep plus that time.

**Configuration.** The API reads `LOG_LEVEL` and `MAX_NOTICE_LENGTH` from its environment.

- A ConfigMap `board-api-config-v1` in `board` (with the recommended labels of the API) holds
  `LOG_LEVEL: info` and `MAX_NOTICE_LENGTH: "500"`, and is `immutable: true`.
- The API container sets both variables with `env` and `valueFrom.configMapKeyRef` (one per
  key, by name): a missing key then stops the Pod from starting instead of passing silently.

An immutable ConfigMap cannot be edited, which is the point: a change is a new
`board-api-config-v2` and a new reference in the Deployment, and that change rolls the Pods
out (and back) like any other release. Pods never read half-changed settings.

> [!TIP]
> Watch a rollout with `kubectl rollout status deploy/board-api -n board` and its history
> with `kubectl rollout history deploy/board-api -n board`.
