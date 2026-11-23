# Sidequest: Notice board on Kubernetes

The team's notice board has two parts: `board-web`, the page people open, and `board-api`,
which stores the notices. Both images are ready (`ghcr.io/acme/board-web:1.9.0` and
`ghcr.io/acme/board-api:2.4.1`); what is missing is everything that runs them on Kubernetes.
You write those manifests, in three stages:

1. two Deployments and their Services, in their own namespace, wired by labels and named
   ports, with the web part finding the API by its DNS name;
2. probes that match how each part starts and fails, and requests and limits picked for a
   QoS class;
3. rollouts and shutdowns that never drop a request, and the API's settings in a ConfigMap.

How it is checked:

- Your work is the `.yaml` files at the top of this folder (one object per file or several
  per file, separated by `---`; the file names are up to you).
- There is no cluster. The dojo reads every `.yaml` file at the top of the folder, validates
  each object with kubeconform against the Kubernetes 1.36 schemas (an unknown or misspelled
  field fails), and runs tests over the objects: names, labels, selectors, ports, probes and
  resources. The rules in each stage's statement are the contract.
- Each stage keeps every earlier check.

The images are fictional, so on a real cluster the Pods would not start, but every object is
accepted: `kubectl apply --dry-run=server -f .` against a local cluster (kind, k3d, minikube)
is a good extra check. When you are done, `[A]` copies a review prompt: ask an AI about your
manifests.
