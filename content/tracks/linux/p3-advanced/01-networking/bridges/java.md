---
from: java
status: draft
concept: "Connecting to other services"
from_way: "A ConnectException or SocketTimeoutException in a stack trace, ipconfig and ping on Windows, and PuTTY for remote access."
native_way: "The same errors seen from the outside: ip and ss for the local side, getent and dig for names, curl -v for the application, and OpenSSH everywhere."
sensei_tip: "java.net.ConnectException: Connection refused and SocketTimeoutException: connect timed out are the same two cases as in curl: nothing listening versus packets dropped. Debug them with nc -zv host port from the same machine as the JVM."
---

| Java / Windows | Linux |
| --- | --- |
| `ipconfig /all` | `ip -br addr`, `ip route`, `resolvectl status` |
| `netstat -ano` | `ss -tlnp` |
| `InetAddress.getByName(host)` | `getent hosts host` |
| `ConnectException: Connection refused` | `nc -zv host port` fails at once |
| `SocketTimeoutException: connect timed out` | `nc -zv` hangs, then times out |
| PuTTY and its saved sessions | `ssh` and `~/.ssh/config` |
