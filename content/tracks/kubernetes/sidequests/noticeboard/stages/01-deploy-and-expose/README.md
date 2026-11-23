Write the manifests that run both parts of the board and let them find each other.

| Object | Rules |
| --- | --- |
| Namespace `board` | every other object lives in it (`metadata.namespace: board`) |
| Deployment `board-api` | 2 replicas; one container `api`, image `ghcr.io/acme/board-api:2.4.1`, port `8080` named `http` |
| Service `board-api` | `ClusterIP`, port `80` named `http`, reaching the API Pods on the **named** port `http` |
| Deployment `board-web` | 2 replicas; one container `web`, image `ghcr.io/acme/board-web:1.9.0`, port `3000` named `http`; the variable `API_URL` points at the API Service |
| Service `board-web` | `ClusterIP`, port `80` named `http`, reaching the web Pods on the port `http` |

Labels follow the Kubernetes recommended set, on every object and on the Pod templates:

- `app.kubernetes.io/name`: `board-api` or `board-web`;
- `app.kubernetes.io/component`: `api` or `web`;
- `app.kubernetes.io/part-of`: `noticeboard` (the Namespace too);
- `app.kubernetes.io/version`: the image tag, on the Deployments and their Pods.

Selectors pick the Pods of one Deployment only: each Deployment's `matchLabels` and each
Service's `selector` match its Pod labels and select no Pod of the other part. They must not
include `app.kubernetes.io/version`: a Deployment's selector cannot change after it is
created, and the version changes at every release.

`API_URL` is `http://` plus a DNS name that reaches the `board-api` Service from the `board`
namespace (`board-api`, `board-api.board` or the full `board-api.board.svc.cluster.local`),
with `:80` or no port.

> [!TIP]
> `kubectl create deployment board-api --image=... --port=8080 --replicas=2 -n board
> --dry-run=client -o yaml` prints a starting point. Name ports: a Service that targets
> `http` keeps working when the container port changes.
