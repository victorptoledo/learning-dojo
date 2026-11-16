# Full Stack Architecture: Monoliths, BFFs and Monorepos

Senior full stack engineers are asked to shape systems, not just features. The Software
Architecture and System Design tracks go deep into styles and distributed systems; this lesson
covers the decisions that come up most in product teams with a web front end, an API and a
database.

## Modular monolith first, services when forced

A **modular monolith** is one deployable with strict internal modules (`checkout`, `catalog`,
`accounts`), each with its own public API and its own tables, and no reaching into another
module's internals. You keep one deploy, one database transaction when you need it, simple local
development and refactoring with the compiler's help.

Split a module into a **service** when a concrete force demands it:

| Force | Example |
| --- | --- |
| independent scaling | image processing needs 40 machines at night, the rest needs 3 |
| independent deploys for separate teams | three teams block each other on one release train |
| isolation of failures or compliance | the payment part must be audited and isolated |
| a different technology | a Python ML model next to a Java API |

Services cost: network calls that fail, data spread over databases (no joins, eventual
consistency, the outbox pattern), distributed tracing, versioned contracts, more pipelines. A
**distributed monolith** (services that must deploy together) has the costs of both and the
benefits of neither. Enforce module boundaries in the monolith first (ArchUnit, Spring Modulith,
Nx or dependency-cruiser rules); a well-bounded module is easy to extract later.

## Backend-for-frontend and gateways

Different clients want different shapes: the web dashboard shows a lot per screen, the mobile app
wants small payloads and fewer round trips, a partner API needs stability for years.

- A **BFF** is a thin backend per client type, owned by that client's team, that aggregates and
  shapes data from the core APIs (and, as in phase 2, can keep OAuth tokens out of the browser).
  Meta-frameworks blur the line: Next.js route handlers and server components, Remix loaders and
  SvelteKit endpoints are BFFs living next to the UI.
- An **API gateway** sits at the edge for cross-cutting concerns: routing, TLS, authentication,
  rate limits. Keep business logic out of it.
- **GraphQL** lets clients ask for the fields they need from one schema; it moves complexity to
  the server (N+1 again: use DataLoader, query cost limits, persisted queries).

## Monorepos and shared contracts

A **monorepo** (pnpm workspaces with Nx or Turborepo, Gradle multi-project, Bazel) holds the web
app, the API and shared packages:

```text
apps/web        apps/api        apps/mobile
packages/contracts   (zod schemas or the OpenAPI spec and generated clients)
packages/ui          (design system)
```

- One change can update the API, the contract and the UI **atomically**, reviewed together.
- **Affected-only builds and tests** keep CI fast as the repository grows.
- **Boundaries** must be enforced by tooling (which package may import which), or the monorepo
  becomes one big ball of mud.
- Polyrepos give harder boundaries and independent tooling, at the cost of coordinating versions
  of shared contracts.

## Organise by feature

Folders by technical layer (`controllers/`, `services/`, `repositories/`, `components/`) spread
one feature over the whole tree. **Vertical slices** keep a feature together (`checkout/` with its
routes, logic, data access and UI), match how teams think, and make ownership clear. Conway's law
says systems mirror communication structures: arrange modules and teams together, with a
`CODEOWNERS` file that says who reviews what.

## Record decisions

Write an **Architecture Decision Record** for decisions that are expensive to change: context,
options, decision, consequences, status. Keep them in the repository next to the code. A year
later, "why do we have a BFF?" has an answer, and so does "what would make us revisit it?".

## Summary

- Start with a modular monolith with enforced boundaries; extract services for concrete forces.
- Use BFFs to shape data per client, gateways for edge concerns, GraphQL with care.
- Monorepos make cross-stack changes atomic; enforce package boundaries and build only what changed.
- Organise by feature and align teams with modules.
- Record expensive decisions in ADRs.

## Further reading

- Martin Fowler, "MonolithFirst": <https://martinfowler.com/bliki/MonolithFirst.html>
- Sam Newman, "Backends For Frontends": <https://samnewman.io/patterns/architectural/bff/>
- Spring Modulith reference documentation: <https://docs.spring.io/spring-modulith/reference/>
- Nx documentation, "Enforce Module Boundaries": <https://nx.dev/docs/features/enforce-module-boundaries>
- Michael Nygard, "Documenting Architecture Decisions": <https://cognitect.com/blog/2011/11/15/documenting-architecture-decisions>
- In the dojo: the Software Architecture track's *The Modular Monolith*, *Microservices and Their Costs* and *Integration Patterns, Sagas and API Gateways*.
