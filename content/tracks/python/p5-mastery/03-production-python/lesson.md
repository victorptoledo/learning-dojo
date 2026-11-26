# Python in Production: Logging, Configuration, Security and Deployment

Code that passes its tests still has to run for months, under load, with secrets, upgrades
and incidents. This lesson collects the practices that make Python services boring to operate.

## Logging

Use the standard `logging` module, never `print`, and follow three rules:

1. **One logger per module**: `log = logging.getLogger(__name__)`. Libraries only create
   loggers; they never configure logging.
2. **Configure once**, at the entry point (the `main` function or the app factory).
3. **Pass values as arguments**, not pre-formatted strings: `log.info("charged %s", order_id)`.
   Formatting happens only if the record is emitted, and structured handlers can keep the
   values as fields.

```python
import logging

log = logging.getLogger(__name__)

def charge(order_id: str) -> None:
    log.info("charging order %s", order_id)
    try:
        gateway.charge(order_id)
    except GatewayError:
        log.exception("charge failed for order %s", order_id)   # ERROR level + traceback
        raise
```

In containers, log to **stdout**, one event per line, preferably as **JSON** (with
`structlog` or a JSON formatter), so the platform can collect and search it. Include
request or trace ids in every line with `contextvars` or a logging filter. Never log
secrets, tokens or full personal data.

| Level | For |
| --- | --- |
| `DEBUG` | Detail for developers, off in production |
| `INFO` | Normal events worth keeping: started, order placed |
| `WARNING` | Something unexpected that was handled: a retry, a fallback |
| `ERROR` | A failed operation (`log.exception` inside `except`) |
| `CRITICAL` | The service cannot continue |

## Configuration and secrets

Follow the twelve-factor rule: **configuration comes from the environment**, not from code.
Validate it once at start-up so a missing variable fails the deploy, not the first request:

```python
from pydantic import PostgresDsn, SecretStr
from pydantic_settings import BaseSettings, SettingsConfigDict

class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_prefix="ORDERS_")
    database_url: PostgresDsn
    api_key: SecretStr             # printed as '**********'
    workers: int = 4
    debug: bool = False
```

Secrets come from the platform's secret store (Kubernetes secrets, a cloud secret manager,
Vault), injected as environment variables or files. A secret that was ever committed to git
is compromised: rotate it, then remove it.

## Quality gates in CI

```text
uv sync --locked                     # fails if uv.lock is out of date
uv run ruff format --check .
uv run ruff check .
uv run mypy --strict src
uv run pytest --cov=orders --cov-branch
uvx pip-audit                        # known vulnerabilities in the installed dependencies
```

Add a bot (Dependabot or Renovate) that opens pull requests for dependency updates, so upgrades
are small and frequent instead of rare and scary. `pre-commit` (or `prek`) runs the fast checks
before each commit.

## Security at the code level

| Risk | Safe practice |
| --- | --- |
| SQL injection | Always use query parameters (`cursor.execute("... WHERE id = %s", (id,))`) or an ORM; never f-strings in SQL |
| Command injection | `subprocess.run([...], check=True)` with a list of arguments; never `shell=True` with user input |
| Unsafe deserialisation | Never `pickle.loads` or `yaml.load` untrusted data; use JSON, `yaml.safe_load` or Pydantic |
| Code execution | No `eval` or `exec` on input |
| Weak randomness | `secrets.token_urlsafe()` for tokens, not `random` |
| Path traversal | Resolve paths and check they stay inside the allowed folder (`path.resolve().is_relative_to(base)`) |
| Supply chain | Locked, hashed dependencies (`uv.lock`), audits, few and well-maintained packages |

Tools such as `bandit` (or ruff's `S` rules) flag many of these automatically.

## Building the container image

```dockerfile
FROM python:3.15-slim AS build
COPY --from=ghcr.io/astral-sh/uv:0.9 /uv /usr/local/bin/uv
ENV UV_COMPILE_BYTECODE=1 UV_LINK_MODE=copy
WORKDIR /app
COPY pyproject.toml uv.lock ./
RUN uv sync --locked --no-dev --no-install-project      # dependencies: a cached layer
COPY src ./src
RUN uv sync --locked --no-dev                           # the project itself

FROM python:3.15-slim
RUN useradd --create-home --uid 10001 app
COPY --from=build --chown=app /app /app
ENV PATH="/app/.venv/bin:$PATH" PYTHONUNBUFFERED=1
USER app
CMD ["uvicorn", "orders.main:app", "--host", "0.0.0.0", "--port", "8000"]
```

The image installs exactly the locked versions, puts dependencies in their own layer (fast
rebuilds), compiles bytecode ahead of time (faster start), contains no build tools or dev
dependencies, and runs as a non-root user. Pin the base image and uv versions, and rebuild
regularly to pick up security patches.

## Processes, workers and shutdown

Each Python process uses one core for Python code (on the standard build), so a service runs
**several worker processes**: Uvicorn's `--workers`, Gunicorn with Uvicorn workers, or one
process per container with the orchestrator scaling replicas (common on Kubernetes). Start with
about one worker per core and measure.

On shutdown, the platform sends **SIGTERM**, waits a grace period, then sends SIGKILL. Servers
such as Uvicorn stop accepting connections and finish in-flight requests on SIGTERM; your own
workers should catch it (`signal.signal` or the asyncio loop's `add_signal_handler`), finish the
current job and exit. Keep `PYTHONUNBUFFERED=1` so logs are not lost in a buffer.

## Observability

- **Health checks**: a cheap liveness endpoint, and a readiness endpoint that checks
  dependencies.
- **Metrics and traces**: OpenTelemetry instruments FastAPI, Django, httpx, SQLAlchemy and
  more with little code, and exports to Prometheus, Grafana, Datadog or similar.
- **Error reporting**: Sentry (or similar) groups exceptions with their context.
- **Live debugging**: `py-spy dump --pid` prints every thread's stack of a stuck process;
  `faulthandler` (`PYTHONFAULTHANDLER=1`) prints tracebacks on crashes and hangs.

## Upgrading Python

Each version is supported for five years, but libraries drop old versions sooner, and every
release is faster. A good rhythm: test the new version in CI when its first bug-fix release
appears (x.y.1), upgrade within the next year, and never run a version past its end of life.
`requires-python` in `pyproject.toml` and the base image tag are the two places to change.

## Summary

- `logging.getLogger(__name__)`, configured once, structured to stdout, `log.exception` in
  handlers, no secrets in logs.
- Configuration from the environment, validated at start-up; secrets from a secret store.
- CI gates: ruff, mypy, pytest with coverage, dependency audits, automated update PRs.
- Parameterised SQL, no `shell=True`, no `pickle` or `eval` on input, `secrets` for tokens.
- Slim, locked, non-root images built with uv; several workers; graceful SIGTERM handling.
- Health checks, OpenTelemetry, error reporting; upgrade Python on a schedule.

## Further reading

- Logging HOWTO: <https://docs.python.org/3/howto/logging.html>
- Logging Cookbook: <https://docs.python.org/3/howto/logging-cookbook.html>
- uv documentation, "Using uv in Docker": <https://docs.astral.sh/uv/guides/integration/docker/>
- Python Packaging User Guide, "Securing package publishing and installation" and related guides: <https://packaging.python.org/en/latest/guides/>
- OpenTelemetry Python: <https://opentelemetry.io/docs/languages/python/>
- Status of Python versions: <https://devguide.python.org/versions/>
- roadmap.sh, Python topic "Package Managers": <https://github.com/nilbuild/developer-roadmap/blob/master/roadmaps/python/content/package-managers@qeCMw-sJ2FR4UxvU9DDzv.md>
