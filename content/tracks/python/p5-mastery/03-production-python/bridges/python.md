---
from: python
status: draft
concept: "From scripts on a server to an operated service"
from_way: "print debugging, logging.basicConfig in every module, settings.py with secrets, pip install on the server, python app.py under screen or nohup, requirements.txt without hashes, and upgrades only when forced."
native_way: "Module loggers configured once with structured output, validated settings from the environment, immutable images built from uv.lock, an ASGI or WSGI server with workers under an orchestrator, CI gates and scheduled upgrades."
sensei_tip: "If deploying means logging in to a server, the first win is an image built in CI from uv.lock: every other improvement (rollbacks, scaling, audits) becomes easier once the artefact is immutable."
---

| Older habit | Production Python |
| --- | --- |
| `print("here", x)` | `log.debug("state %s", x)` |
| `logging.basicConfig()` in many modules | configure once at the entry point |
| `DB_PASSWORD = "..."` in code | `SecretStr` from the environment or a secret store |
| `pip install -r requirements.txt` on the server | an image built from `uv.lock` |
| `nohup python app.py &` | Uvicorn or Gunicorn workers under systemd, Docker or Kubernetes |
| `yaml.load(f)` | `yaml.safe_load(f)` |
| `random.choice` for tokens | `secrets.token_urlsafe()` |
| upgrading Python when the OS forces it | a scheduled upgrade after each x.y.1 |
