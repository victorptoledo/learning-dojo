# Hello, Pod

Write `pod.yaml`: a single Pod that prints `hello, dojo` once and stops.

| Rule | Value |
| --- | --- |
| kind and API version | `Pod`, `v1` |
| name | `hello` |
| labels | `app: hello` and `track: kubernetes` |
| one container | named `hello`, image `busybox:1.37` |
| what it runs | the command `echo hello, dojo` (as a list: `["echo", "hello, dojo"]`) |
| after it exits | it is not restarted: `restartPolicy: Never` |

How the check works: the dojo renders the `.yaml` files of the folder, validates every object
with **kubeconform** (an unknown field or a wrong type fails the `kubeconform::manifests`
test), then runs the bats tests over the objects.

On a cluster:

```powershell
kubectl apply -f pod.yaml
kubectl get pod hello        # STATUS Completed
kubectl logs hello           # hello, dojo
kubectl delete -f pod.yaml
```

There are 8 tests: 1 in `tests/visible.bats`, 6 hidden ones and the kubeconform validation.
