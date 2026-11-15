---
from: python
status: draft
concept: "Describing a multi-service environment"
from_way: "python-dotenv loads a .env file, and a Makefile or README explains how to start Postgres and Redis locally."
native_way: "compose.yaml starts the app, Postgres and Redis together; .env interpolates the file and env_file or environment configure the containers."
sensei_tip: "Django and SQLAlchemy apps should retry the first database connection: depends_on with service_healthy helps at start-up, but not when the database restarts later."
---

| Python | Compose |
| --- | --- |
| `python-dotenv` and `.env` | `env_file:` or `environment:` |
| `celery worker` in another terminal | a `worker` service using the same image |
| `manage.py migrate` before starting | a one-off service with `service_completed_successfully` |
| `os.environ["DB_PASSWORD"]` | read `/run/secrets/db_password` instead |
