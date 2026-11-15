# Checkpoint 2: Intermediate

This checkpoint closes the **Intermediate** phase. It has two parts, and you need both:

1. **The quiz** (press **Q**): ten questions over the whole phase. You need 8 right.
2. **The integration kata** (press **Enter**): write the production overlay of a service with
   Kustomize: replicas, image, resources, configuration, spreading over zones, a disruption
   budget and a Gateway API route.

Do them in any order. Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| Scheduling | Why is a Pod Pending? Taint or affinity? What does maxSkew 1 do? What does a PDB protect against? |
| Storage | PVC or PV? What does ReadWriteOnce limit? What does a StatefulSet give each replica? |
| Ingress and the Gateway API | Who implements an Ingress? Which Gateway API object does an app team own? |
| Jobs, CronJobs and DaemonSets | Which restart policies may a Job use? What does concurrencyPolicy Forbid do? |
| Kustomize and Helm | How does an overlay reach its base? Why the hash suffix on generated ConfigMaps? What is a release? |

> [!TIP]
> If a quiz question surprises you, open the lesson it comes from again before retrying. The
> explanation after each answer says which idea it tests.
