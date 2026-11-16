---
from: go
status: reviewed
concept: "A static binary is not yet a release"
from_way: "go build, scp the binary, restart a systemd unit, flags passed by hand."
native_way: "A distroless image per commit, configuration from the environment, secrets mounted as files, migrations as a one-shot job, health endpoints for the platform."
sensei_tip: "Go's fast start makes it a good fit for serverless and scale-to-zero platforms; the database connection limit is then your bottleneck, not the cold start."
---

| Go habit | Deployment practice |
| --- | --- |
| flags typed on the server | environment variables |
| migrations in `main()` | a one-shot migrate job |
| `scp` the binary | an image tagged by commit |
| `sql.DB` per function instance | `SetMaxOpenConns` and a pooler |
