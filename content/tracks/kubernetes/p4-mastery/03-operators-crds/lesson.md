# Extending Kubernetes: CRDs, Controllers and Operators

Kubernetes is not only a container scheduler; it is a framework for writing **declarative
APIs**. The same API server, storage, RBAC, watches and kubectl that serve Deployments can
serve your own kinds, and the same reconciliation loop can manage anything: databases,
certificates, DNS records, cloud resources.

## CustomResourceDefinitions

A **CRD** adds a new resource type to the API:

```yaml
apiVersion: apiextensions.k8s.io/v1
kind: CustomResourceDefinition
metadata:
  name: databases.platform.acme.io       # <plural>.<group>
spec:
  group: platform.acme.io
  scope: Namespaced
  names: { kind: Database, plural: databases, singular: database, shortNames: [db] }
  versions:
    - name: v1alpha1
      served: true
      storage: true
      subresources:
        status: {}                        # status is written separately from spec
      additionalPrinterColumns:
        - { name: Ready, type: string, jsonPath: '.status.conditions[?(@.type=="Ready")].status' }
      schema:
        openAPIV3Schema:
          type: object
          properties:
            spec:
              type: object
              required: [version, size]
              properties:
                version: { type: string, enum: ["17", "18"] }
                size: { type: string, enum: [small, medium, large] }
            status:
              type: object
              x-kubernetes-preserve-unknown-fields: true
```

Now `kubectl get databases`, RBAC rules on `databases`, watches and `kubectl explain` all
work. The **schema** validates every object (CEL rules with `x-kubernetes-validations` add
cross-field checks), and several **versions** can be served at once, with a conversion
webhook when their shapes differ. A CRD alone stores data and does nothing else.

```yaml
apiVersion: platform.acme.io/v1alpha1
kind: Database
metadata: { name: orders, namespace: shop }
spec: { version: "18", size: small }
```

## Controllers and the operator pattern

A **controller** watches the custom resources and reconciles the world towards them, exactly
like the built-in ones (Phase 1). An **operator** is a controller that encodes the
operational knowledge of a specific piece of software: how to install it, upgrade it, back
it up, fail over, resize. CloudNativePG (PostgreSQL), Strimzi (Kafka), cert-manager and the
Prometheus Operator are well-known examples.

A reconcile function, in the controller-runtime style:

```text
reconcile(namespace/name):
    db := get Database               → gone? return (children are garbage-collected)
    if being deleted: run cleanup, remove the finalizer, return
    ensure the finalizer is set
    ensure StatefulSet, Service and Secret exist and match the spec   (create or patch)
    read their status, set db.status.conditions and observedGeneration
    requeue if not ready yet
```

Rules that make controllers robust:

- **Idempotent and level-triggered**: reconcile from the current state every time; never
  assume you saw every event.
- **Owner references** on every child (`metadata.ownerReferences` pointing at the
  Database) let the garbage collector delete them with the parent, and let the controller
  watch them.
- **Finalizers** (`metadata.finalizers`) block deletion until the controller has cleaned up
  things outside the cluster (a cloud database, a DNS record, a final backup).
- **Status conditions** (`Ready`, `Progressing`, `Degraded`, with reasons and messages) tell
  users and tools what happened.
- **Leader election**: run two replicas, only one active.

Frameworks do the plumbing: **kubebuilder** and **controller-runtime** (Go), the
**Operator SDK**, **kopf** (Python), **Java Operator SDK** and **KubeOps** (.NET).

## Admission webhooks and policies

To change or reject objects as they are created, Kubernetes calls **admission webhooks**:
mutating ones (inject a sidecar, set defaults) and validating ones (enforce rules). They run
on the critical path of every matching request, so a slow or broken webhook can block the
cluster: set `failurePolicy`, timeouts and narrow selectors. For many rules the built-in
**ValidatingAdmissionPolicy** with CEL expressions, or a policy engine like Kyverno, avoids
writing a webhook at all.

## When to write an operator

Build one when the software has real operational logic that must react continuously and you
can maintain it for years. Otherwise prefer, in order: a managed service, a mature
open-source operator, a Helm chart, and only then your own code. Installing an operator
means trusting it: it usually needs wide RBAC (often cluster-wide), it runs webhooks in the
request path, and its CRDs must be upgraded carefully, because removing a CRD deletes every
object of that kind.

## Summary

- A CRD adds a kind with a schema, versions, a status subresource and printer columns.
- A controller reconciles custom resources; an operator encodes how to run specific software.
- Owner references, finalizers, status conditions and idempotent reconciles make
  controllers safe.
- Admission webhooks and ValidatingAdmissionPolicies mutate or validate objects on the way in.
- Prefer managed services and mature operators; your own operator is a long-term commitment.

## Further reading

- Custom resources: <https://kubernetes.io/docs/concepts/extend-kubernetes/api-extension/custom-resources/>
- Extend the API with CRDs: <https://kubernetes.io/docs/tasks/extend-kubernetes/custom-resources/custom-resource-definitions/>
- Operator pattern: <https://kubernetes.io/docs/concepts/extend-kubernetes/operator/>
- The Kubebuilder book: <https://book.kubebuilder.io/>
- Dynamic admission control: <https://kubernetes.io/docs/reference/access-authn-authz/extensible-admission-controllers/>
