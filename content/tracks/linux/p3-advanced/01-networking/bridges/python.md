---
from: python
status: draft
concept: "Connecting to other services"
from_way: "requests raising ConnectionError or Timeout, socket for low-level work, and ipconfig or ping on Windows."
native_way: "The same failures seen from the outside: ip and ss locally, getent and dig for names, curl -v for the application."
sensei_tip: "requests has no timeout by default and can hang forever on dropped packets. Always pass timeout=(3, 10); a ConnectTimeout means dropped packets, a ConnectionRefusedError that nothing listens."
---

| Python | Linux |
| --- | --- |
| socket.gethostbyname(h) | getent hosts h |
| requests.exceptions.ConnectTimeout | nc -zv host port times out |
| ConnectionRefusedError | nc -zv fails at once |
| app.run(host='127.0.0.1') | listening on loopback only, ss -tlnp |
| paramiko | ssh and ~/.ssh/config |
