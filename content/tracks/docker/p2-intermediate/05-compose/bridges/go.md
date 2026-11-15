---
from: go
status: draft
concept: "Describing a multi-service environment"
from_way: "A Makefile with targets that docker run the database, and flags or environment variables for the service's configuration."
native_way: "compose.yaml replaces the Makefile targets: one file builds the Go service, starts its dependencies, wires configuration and waits for health."
sensei_tip: "Read secrets from a file path in an environment variable (API_TOKEN_FILE=/run/secrets/api_token) so the same binary works with Compose, Kubernetes and local files."
---

| Go | Compose |
| --- | --- |
| `make db-up` | `docker compose up -d db` |
| `flag` and `os.Getenv` | `environment:` and `command:` |
| reading a token from a file | a Compose secret under `/run/secrets` |
| a retry loop for the first DB connection | still needed, even with `service_healthy` |
