# Checkpoint 4: Final Exam

This checkpoint closes the **Mastery** phase and the whole track. It has two parts, and you
need both:

1. **The quiz** (press **Q**): eight questions over the advanced and mastery phases:
   security, autoscaling, debugging, operators and production. You need 7 right.
2. **The integration kata** (press **Enter**): make the Helm chart of a `Database` operator
   ready for production. The CRD gets a strict schema, a CEL rule, a stored `v1` next to a
   deprecated `v1alpha1`, the status subresource and printer columns; the controller loses
   `cluster-admin` and gets exactly the RBAC it reconciles, plus a Lease for leader election;
   the built-in roles learn the new kind through aggregation; and the release refuses to
   render without an image digest or with two controllers and no leader election.

Do them in any order. Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| Debugging Workloads | Pending, CrashLoopBackOff or not ready: where do I look first? What does an empty endpoint list mean? |
| Fix a Broken Release (kata) | Which symptom points at the probe, which at the selector, which at the limit? |
| Extending Kubernetes: CRDs, Controllers and Operators | What does a structural schema reject? Why the status subresource? What do ownerReferences and finalizers do? Which versions are served, and which one is stored? |
| Kubernetes in Production | Which order for an upgrade? What breaks when an API is removed? Why digests and GitOps? What makes a namespace safe for a team? |
| Authentication, RBAC and ServiceAccounts | Which verbs on which subresources does a controller need? Why is a Role enough for a Lease? How does aggregation extend `edit` and `view`? |

> [!TIP]
> If a quiz question surprises you, open the lesson it comes from again before retrying. The
> explanation after each answer says which idea it tests.
