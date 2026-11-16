# Deploying the Stack: Containers, Environments, CD and Serverless

A feature is done when users have it. Deploying a full stack product means shipping at least two
things that must agree (a front-end bundle and an API) plus a database whose schema changes with
them. The Docker and Git tracks teach the tools; this lesson is about the whole path.

## Build once, promote everywhere

```text
merge → CI: test, build → artifacts tagged by commit (shop-api:3f9a1c2, web bundle 3f9a1c2)
      → deploy to staging → smoke tests → promote the SAME artifacts → production
```

- **One artifact per commit.** Never rebuild for production: what you tested is what you ship.
- **Tag by commit or version**, never `latest`, so every environment can say exactly what runs.
- **Environments differ only in configuration**, which comes from outside the artifact.

## Configuration and secrets

Follow the twelve-factor rule: configuration in the environment.

- Plain settings (`DATABASE_HOST`, `CORS_ORIGIN`, feature flags) come from environment variables
  or a config service.
- **Secrets** (database passwords, API keys) come from a secret store (Vault, AWS Secrets
  Manager, Kubernetes Secrets, Compose secrets) and are mounted as files or injected at start;
  never in the image, the repository or the front-end bundle.
- **The front end is public.** Anything built into the JavaScript bundle (`VITE_*`,
  `NEXT_PUBLIC_*`) is readable by every visitor. Only public values belong there.
- A front end that must run in several environments without a rebuild can load a small
  `/config.json` at start instead of baking the API URL in.

## Migrations in the pipeline

Run migrations **once, before** the new version starts: a pipeline step, a Kubernetes Job, or a
one-shot Compose service that the API waits for (`service_completed_successfully`). Keep each
migration compatible with the version still running (add before use, remove after nobody uses
it): during a rolling deploy, old and new code share the database, and a rollback of the code
must not need a rollback of the schema.

## Where it runs

| Option | You manage | Good for |
| --- | --- | --- |
| **Containers** on a VM with Compose | the host, updates, scaling | small products, staging, self-hosting |
| **Container platforms** (Kubernetes, ECS, Cloud Run, Fly.io, Render) | the container and its config | most APIs |
| **Static hosting + CDN** (S3 + CloudFront, Netlify, Vercel, Cloudflare Pages) | the files | SPAs, SSG sites |
| **Serverless functions** (AWS Lambda, Azure Functions, Vercel/Netlify functions) | the code | spiky or low traffic, glue, webhooks |
| **Edge functions** (Cloudflare Workers, Vercel Edge, Lambda@Edge) | small code close to users | redirects, A/B tests, auth checks, personalisation |

Containers give the same artifact on a laptop and in production (the Compose kata builds that
stack). Platforms add health checks, rolling deploys and autoscaling for you.

## Serverless and the database

Serverless functions scale to zero and start on demand, which changes two things:

- **Cold starts**: the first request to a new instance waits for the runtime and your code to
  load. Keep bundles small, prefer fast-starting runtimes, and use provisioned concurrency for
  latency-critical paths.
- **Connections**: a thousand concurrent functions may each open a database connection, and
  PostgreSQL handles a few hundred at best. Put a **connection pooler** in front (PgBouncer,
  RDS Proxy, Supabase's or Neon's poolers) or use an HTTP-based serverless driver. Open the
  client outside the handler so warm instances reuse it.

Edge functions run in lightweight isolates with limited APIs, often far from your database:
keep them to logic that needs no database, or use a globally replicated store.

## Releasing safely

- **Health checks**: a liveness check (the process is alive) and a readiness check (it can serve:
  database reachable, migrations done).
- **Rolling, blue-green or canary deploys**, so a bad version reaches few users before you notice.
- **Feature flags** decouple deploying code from releasing a feature (phase 4).
- **Rollback** is redeploying the previous artifact, which works because migrations are
  compatible.
- **Static assets**: keep the previous bundle's hashed files for a while, because open tabs still
  request them after a deploy.

## Summary

- Build one artifact per commit, tag it, promote it unchanged.
- Configuration in the environment, secrets in a store, nothing secret in the front-end bundle.
- Run compatible migrations once, before the new version.
- Pick containers, platforms, static hosting, serverless or edge per part of the product;
  serverless needs a connection pooler.
- Release with health checks, gradual rollouts and rollbacks that do not touch the schema.

## Further reading

- The Twelve-Factor App: <https://12factor.net/>
- Docker documentation, "Compose file reference": <https://docs.docker.com/reference/compose-file/>
- AWS documentation, "Lambda execution environment lifecycle" (cold starts): <https://docs.aws.amazon.com/lambda/latest/dg/lambda-runtime-environment.html>
- PgBouncer documentation: <https://www.pgbouncer.org/usage.html>
- Martin Fowler, "BlueGreenDeployment": <https://martinfowler.com/bliki/BlueGreenDeployment.html>
