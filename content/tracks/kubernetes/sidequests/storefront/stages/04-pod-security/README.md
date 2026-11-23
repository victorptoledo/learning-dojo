The network now limits where an attacker can go. Limit what they can do inside a Pod: no
root, no new privileges, no writable image, no Kubernetes credentials.

**The `restricted` Pod Security Standard**, for all three workloads and every container:

- `runAsNonRoot: true` with a fixed non-zero user and group: `10001` for `web` and `api`,
  `999` (the `postgres` user of the image) for `db`;
- `seccompProfile: { type: RuntimeDefault }` on the Pod;
- on each container, `allowPrivilegeEscalation: false` and `capabilities: { drop: [ALL] }`;
- no privileged containers, host namespaces or `hostPath` volumes (none today; keep it so).

**Read-only roots.** Every container sets `readOnlyRootFilesystem: true`, and gets a writable
volume only where it must write, each an `emptyDir` with a `sizeLimit` (an unbounded one can
fill the node's disk):

| Workload | Writable paths |
| --- | --- |
| `web` | `/tmp` (64Mi) |
| `api` | `/tmp` (64Mi) |
| `db` | `/tmp` (64Mi), `/var/run/postgresql` (16Mi, the socket and lock file) and the data volume at `/var/lib/postgresql` |

The database's volume must be writable by the `postgres` user: set `fsGroup: 999` and
`fsGroupChangePolicy: OnRootMismatch` (Kubernetes then fixes the volume's ownership once, not
on every restart of a large volume).

**Identities.** One ServiceAccount per part, `web`, `api` and `db` (for example in
`serviceaccounts.yaml`), each with `automountServiceAccountToken: false`; each workload runs
as its own account and its Pods set `automountServiceAccountToken: false` too. None of them
calls the Kubernetes API, so none needs a token: a stolen token is a foothold in the cluster.

Images keep their exact tags.

> [!TIP]
> Turning on a read-only root usually breaks something that writes to an odd place. On a
> cluster, `kubectl logs` shows the `Read-only file system` error with the path to mount.
