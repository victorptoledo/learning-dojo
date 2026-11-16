---
from: go
status: reviewed
concept: "Owning a Go service in production"
from_way: "A breaking SQL migration in the same release as the code, and a restart as the only incident response."
native_way: "Additive migrations with golang-migrate, backfills as a separate command, flags through OpenFeature's Go SDK, and runbooks that start with rollback."
sensei_tip: "golang-migrate wraps each file in a transaction only if you write BEGIN and COMMIT; CREATE INDEX CONCURRENTLY must stay outside a transaction."
---

| Go | Production ownership |
| --- | --- |
| `ALTER TABLE ... RENAME` | expand, migrate, contract |
| a `backfill` subcommand | batched data changes |
| OpenFeature Go SDK | feature flags |
| redeploy the previous image tag | the first mitigation |
