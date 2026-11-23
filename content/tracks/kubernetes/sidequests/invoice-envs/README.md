# Sidequest: Invoices in every environment

The billing team runs its `invoices` service in three places: dev (one Pod, debug logs, a
release candidate), staging and production. Today that is three copies of the same YAML
that drift apart a little more at every release. You replace them with one Kustomize base
and small overlays, in four stages:

1. a **base** with the configuration in generated ConfigMaps, whose names change when their
   content does (so a config change rolls the Pods);
2. **overlays** for dev and production that change only what differs: namespace, image tag,
   replicas, log level, resources and where the database password comes from;
3. a **component** with the public endpoint (Ingress, TLS and a PodDisruptionBudget) that
   staging and production opt into and dev does not;
4. **replacements** that copy the public host into the app's settings, an image pinned by
   digest, and proof that production carries nothing from dev.

How it is checked:

- Your work is the folders `base/`, `overlays/<env>/` and `components/` (stage by stage, the
  statement says which). `kustomize` 5.8 is the tool; `kubectl kustomize` works the same.
- There is no cluster. The dojo runs `kustomize build` on the base and on every overlay the
  stage names, validates the output with kubeconform against the Kubernetes 1.36 schemas,
  and runs tests over the rendered objects. Each stage keeps every earlier check.
- Try it yourself with `kustomize build overlays/prod` (or `kubectl kustomize
  overlays/prod`), and `diff` two overlays to see exactly what differs.

When you are done, `[A]` copies a review prompt: ask an AI about your kustomizations.
