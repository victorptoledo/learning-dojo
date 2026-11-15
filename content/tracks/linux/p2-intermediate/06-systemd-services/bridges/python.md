---
from: python
status: draft
concept: "Running an application as a service"
from_way: "gunicorn or uvicorn started by hand, nohup, supervisord, or a Windows service wrapper."
native_way: "A unit file: systemd starts the process as its own user, restarts it, stops it with SIGTERM and keeps its output in the journal."
sensei_tip: "Run gunicorn or uvicorn in the foreground from the venv's path (ExecStart=/srv/app/.venv/bin/gunicorn ...) and set PYTHONUNBUFFERED=1 so log lines reach the journal at once."
---

| Python | Linux |
| --- | --- |
| supervisord | systemd service units |
| nohup python app.py & | systemctl start app |
| logging to app.log | stdout plus journalctl -u app |
| celery beat or cron | a .timer unit |
| python-dotenv | EnvironmentFile= |
