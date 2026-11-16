---
from: python
status: draft
concept: "Diagnose a service from the host"
from_way: "You restart `python manage.py runserver` in a tmux session when it hangs."
native_way: "gunicorn runs as a systemd service with Restart=on-failure; you read journalctl, check workers with ps and ports with ss."
sensei_tip: "py-spy dump --pid PID shows what every Python thread is doing right now, without restarting the process."
---

| Python world | Linux |
| --- | --- |
| a tmux session | a systemd unit |
| `print` to the terminal | the journal (`journalctl -u`) |
| `signal.signal(SIGTERM, ...)` | graceful stop |
| `psutil` | `ps`, `/proc`, `free`, `df` |
