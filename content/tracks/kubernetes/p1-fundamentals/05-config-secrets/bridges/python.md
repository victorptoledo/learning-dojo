---
from: python
status: draft
concept: "Configuration outside the artifact"
from_way: "A settings.py per environment and a .env file read by python-dotenv, sometimes committed by mistake."
native_way: "ConfigMaps and Secrets provide the same values as environment variables or files; nothing sensitive lives in the image or in Git."
sensei_tip: "pydantic-settings reads env vars and, with secrets_dir, one file per key: point it at the mounted Secret folder."
---

| Python world | Kubernetes |
| --- | --- |
| `.env` + python-dotenv | `envFrom` a ConfigMap |
| `os.environ["DB_PASSWORD"]` | `secretKeyRef` |
| `settings_prod.py` | a ConfigMap per environment (or a Kustomize overlay) |
| reading `/run/secrets/...` | a Secret volume |
