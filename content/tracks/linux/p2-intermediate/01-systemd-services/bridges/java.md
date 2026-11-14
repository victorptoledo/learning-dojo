---
from: java
status: draft
concept: "Running processes in production"
from_way: "Starting `nohup ./app &` and hoping it survives when the terminal closes."
native_way: "Write `/etc/systemd/system/app.service` with `Restart=always` and manage it with `systemctl`."
sensei_tip: "Use `journalctl -u app.service -f` to follow the service's logs live with system timestamps."
---
