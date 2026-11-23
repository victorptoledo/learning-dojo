Staging and production are reachable from outside; dev is not. The public endpoint is a
feature that some environments switch on, which is what a Kustomize **component** is for: a
reusable bundle of resources and patches (`kind: Component`) that an overlay lists under
`components:`.

**`components/public/`** (`apiVersion: kustomize.config.k8s.io/v1alpha1`, `kind: Component`)
adds:

- an Ingress `invoices` with `ingressClassName: nginx`, one rule for a placeholder host
  (`invoices.example.test`) whose path `/` (`Prefix`) goes to the Service `invoices` on its
  port named `http`, and TLS for the same host with the Secret `invoices-tls`;
- a PodDisruptionBudget `invoices` with `maxUnavailable: 1`, selecting the invoices Pods
  (a node drain then takes at most one Pod at a time).

**A new overlay `overlays/staging`**: namespace `invoices-staging`, image tag `3.0.4` (staging
runs what production will run), 2 replicas, label `environment: staging`, the base's log
level, no Secret (the platform creates it, as in prod), and the component.

**Hosts.** Each overlay that uses the component sets its own host, in the rule and in the TLS
entry, with a patch on the Ingress (a JSON 6902 patch with `op: replace` on
`/spec/rules/0/host` is the shortest): `invoices.staging.acme.test` in staging and
`invoices.acme.test` in production.

`overlays/prod` uses the component too; `overlays/dev` does not, and renders no Ingress and
no PodDisruptionBudget.

The dojo builds the three overlays and checks them with kubeconform.

> [!TIP]
> A base cannot be "switched off" in an overlay, but a component can be left out. Anything
> optional across environments (an Ingress, a sidecar, debug settings) belongs in one.
