# Checkpoint 1: Fundamentals

This checkpoint closes the **Fundamentals** phase. It has two parts, and you need both:

1. **The quiz** (press **Q**): ten questions over the whole phase. You need 8 right.
2. **The integration kata** (press **Enter**): deploy a small web service with a ConfigMap,
   a Deployment and a Service that fit together.

Do them in any order. Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| The Control Plane | Who stores state, who schedules, who creates Pods? What does level-triggered mean? |
| Pods | What do containers in a Pod share? Init container or sidecar? What happens on deletion? |
| Deployments | Which changes start a rollout? What do `maxSurge` and `maxUnavailable` guarantee? |
| Services and DNS | `port` or `targetPort`? Why would a Service have no endpoints? Which DNS name from another namespace? |
| ConfigMaps and Secrets | Which updates reach a running Pod? What actually protects a Secret? |
| Probes and Resources | Which probe restarts, which removes from traffic? What do exit code 137 and a CPU limit mean? |

> [!TIP]
> If a quiz question surprises you, open the lesson it comes from again before retrying. The
> explanation after each answer says which idea it tests.
