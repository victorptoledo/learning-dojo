# Platform Engineering: Golden Paths and Internal Platforms

"You build it, you run it" works beautifully for five teams. With fifty, every team writes its
own pipelines, Terraform, Kubernetes manifests, dashboards and on-call tooling, and every team
learns the same lessons the hard way. **Platform engineering** builds an internal platform
that gives product teams those capabilities as self-service products, so they keep ownership
without carrying everything.

## Cognitive load

The book *Team Topologies* frames the problem as **cognitive load**: a team can only hold so
much in its head. If shipping a feature needs expertise in Kubernetes, Terraform, IAM, service
meshes, observability pipelines and compliance, the feature work slows down and mistakes go
up. A platform removes **extraneous** load (how to wire the tools together) so teams can spend
their attention on the **intrinsic** load of their own domain.

## The platform as a product

An internal developer platform (IDP) is a product whose customers are developers:

- **know your users**: interview teams, watch them create a service, measure where the weeks
  go;
- **start thin**: the "thinnest viable platform" may be a wiki page and a template; grow it
  from demand;
- **self-service, not tickets**: if using the platform means filing a request, it is a ticket
  queue with a new name;
- **optional but compelling**: teams adopt the platform because it is the easiest way, not
  because they are forced; mandates breed workarounds;
- **documentation, support and a roadmap**, like any product.

## Golden paths

A **golden path** (or paved road) is the supported, opinionated way to do a common task, with
the best practices built in:

```text
"create a new HTTP service" golden path
  template repository   language skeleton, Dockerfile, tests, linting
  CI/CD                 the organisation's reusable workflows, pinned and secure
  infrastructure        Terraform modules for its database, queue and DNS
  Kubernetes            a Helm chart or Kustomize base with probes, limits, PDB
  observability         metrics, logs and traces wired to the standard dashboards and SLOs
  security              scanning, signing, least-privilege IAM, secrets from the vault
  catalogue entry       owner, on-call, docs, dependencies
```

Teams may leave the path for a good reason, but then they own what they build. The path stays
attractive by being faster than the alternative.

Typical platform capabilities: environments on demand (including per pull request), reusable
pipelines, infrastructure modules, a service catalogue, secrets, observability, cost
visibility and policy guard rails.

## Developer portals

A **developer portal** (Backstage is the best known; Port and Cortex are products) is the front
door: a **service catalogue** (who owns what, its docs, its dependencies, its health),
**software templates** that create a new service from a golden path, and plugins that show CI,
deployments, alerts and costs in one place. A portal is a window onto the platform, not the
platform itself: a portal over nothing is an empty shop.

## Team topologies

*Team Topologies* names four team types and three interaction modes:

| Team type | Role |
| --- | --- |
| **stream-aligned** | delivers a product or service end to end; most teams |
| **platform** | provides internal services that reduce stream-aligned teams' load |
| **enabling** | coaches teams in new skills (SRE practices, security), then moves on |
| **complicated-subsystem** | owns a part that needs rare expertise (a video codec, a pricing engine) |

The platform team interacts mostly in **X-as-a-service** mode (self-service products), and in
**collaboration** mode while discovering a new capability with a pilot team. **Facilitating**
is the enabling teams' mode.

## Measuring the platform

- **Adoption**: how many services and teams use each capability, and how many leave the path.
- **Speed**: time from "new service" to its first production deploy; DORA metrics of teams on
  the platform compared with those off it.
- **Reliability and security** of the platform itself (it is production for your developers),
  and the number of repeated findings it prevents.
- **Developer experience**: regular surveys and satisfaction scores (frameworks such as SPACE
  and DevEx combine perception and system data).

## Summary

- At scale, cognitive load slows teams; a platform turns shared needs into self-service
  products.
- Treat the platform as a product: user research, a thin start, self-service, optional but
  compelling adoption.
- Golden paths bundle best practices for common tasks; a portal such as Backstage is the front
  door.
- Team Topologies: stream-aligned, platform, enabling and complicated-subsystem teams.
- Measure adoption, speed (DORA, time to first deploy), platform reliability and developer
  experience.

## Further reading

- *Team Topologies*, 2nd edition, Matthew Skelton and Manuel Pais (IT Revolution, 2025)
- CNCF Platforms white paper: <https://tag-app-delivery.cncf.io/whitepapers/platforms/>
- Backstage documentation: <https://backstage.io/docs/>
- platformengineering.org: <https://platformengineering.org/>
- Evan Bottcher, "What I Talk About When I Talk About Platforms": <https://martinfowler.com/articles/talk-about-platforms.html>
