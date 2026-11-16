# Checkpoint 2: Intermediate

This checkpoint closes the **Intermediate** phase. It has two parts, and you need both:

1. **The quiz** (press **Q**): ten questions over the whole phase. You need 8 right.
2. **The integration kata** (press **Enter**): turn a one-service Terraform configuration into
   one that creates a module per service with `for_each`, move the already-applied service
   without destroying it, and write the pipeline that plans each pull request and applies
   `main` in a protected environment with OIDC credentials.

Do them in any order. Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| Kubernetes for operations | What do readiness probes and maxUnavailable protect? Which commands find why a Pod crashes? |
| Continuous delivery and GitOps | Why promote one artefact? What do prune and selfHeal do? How do you roll back with GitOps? |
| Infrastructure as code | What is in state and why lock it? Why for_each over count? What does a moved block do? |
| A Terraform module | How do validations reject input? How does for_each change a resource's address? |
| Release strategies | Deploy or release? When blue-green, when canary? Why expand and contract? |
| A canary release | Why must each Service select exactly one track? How are weights turned into shares? |

> [!TIP]
> If a quiz question surprises you, open the lesson it comes from again before retrying. The
> explanation after each answer says which idea it tests.
