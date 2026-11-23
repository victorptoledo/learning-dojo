A chart does more than render objects: it runs steps around a release, checks the release
works, and tells the person installing it what to do next.

**A migration hook.** Each release of the page may change its database, so the migration
runs before the new Pods start. With the new value `migrations.enabled: true` (the default;
add it to the schema), the chart renders a Job `<fullname>-migrate`:

- annotations `helm.sh/hook: pre-install,pre-upgrade`, `helm.sh/hook-weight: "-5"` and
  `helm.sh/hook-delete-policy: before-hook-creation,hook-succeeded` (an old Job never blocks
  the next upgrade, and a failed one stays for `kubectl logs`);
- the page's image (the same tag as the Deployment), `args: ["migrate"]`, `restartPolicy:
  Never`, `backoffLimit: 1`, `activeDeadlineSeconds: 300`, and the `resources` from the
  values;
- **nothing the chart itself creates.** A `pre-install` hook runs before any of the release's
  objects exist, so the Job cannot use the chart's ServiceAccount or ConfigMap: it runs as
  the namespace's `default` account (no `serviceAccountName`, or `default`), mounts no token
  (`automountServiceAccountToken: false`) and has no `envFrom`.

`--set migrations.enabled=false` renders no Job.

**A test.** `templates/tests/test-connection.yaml` is a Pod `<fullname>-test` with
`helm.sh/hook: test` (and the same delete policy) that `helm test` starts after an install:
image `busybox:1.37.0`, `restartPolicy: Never`, small resources (`cpu: 10m`, `memory: 16Mi`,
memory limit `32Mi`), and the command `wget -qO- http://<fullname>:<service port>/health`.

**Release notes.** `templates/NOTES.txt` is printed after `helm install` and `upgrade`:

- with the Ingress on, a line `Status page: https://<host>` (`http://` when TLS is off);
- without it, a line `kubectl --namespace <namespace> port-forward svc/<fullname> 8080:<service port>`
  and then `Status page: http://localhost:8080`.

**Lint.** `helm lint --strict chart` passes, with the defaults and with the Ingress on.

The dojo reads the notes from `helm install sp chart --dry-run=client`, which needs no
cluster.

> [!TIP]
> Hooks are not part of the release's objects: `helm uninstall` leaves a Job that the delete
> policy did not remove. `helm get hooks sp` shows them on a cluster.
