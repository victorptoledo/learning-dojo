# Checkpoint 3: Advanced

This checkpoint closes the **Advanced** phase. It has two parts, and you need both:

1. **The quiz** (press **Q**): eight questions over the whole phase. You need 7 right.
2. **The integration kata** (press **Enter**): finish the Helm chart of a queue worker. It
   gets its own ServiceAccount and a Role with exactly the API calls it makes, runs under the
   restricted profile with a read-only root, scales on the depth of its queue instead of on
   CPU, and is fenced by a NetworkPolicy that still lets it reach RabbitMQ, the DNS and the
   API server.

Do them in any order. Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| Authentication, RBAC and ServiceAccounts | Role or ClusterRole? Who is the subject of a Pod's API calls? Why is `pods/log` its own rule? Which verbs let someone escalate? |
| Security Contexts, Pod Security Standards and NetworkPolicy | What does restricted demand? Where does a read-only app write? One peer with two selectors, or two peers? What does a rule without `ports` allow? |
| Lock Down a Namespace (kata) | Which flows did the default deny break, and which rule gives each one back? |
| Horizontal Pod Autoscaler (HPA) and KEDA | Why is CPU the wrong signal for a queue worker? What do `AverageValue` and the stabilization window do? |
| A Production Release with Helm (kata) | Who owns `replicas` when an HPA exists? How does a value turn a whole template on or off? |

> [!TIP]
> If a quiz question surprises you, open the lesson it comes from again before retrying. The
> explanation after each answer says which idea it tests.
