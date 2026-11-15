---
from: javascript
status: draft
concept: "Who may do what with a file"
from_way: "fs.chmod with octal numbers, and rarely more: npm and Node apps usually run as the logged-in user."
native_way: "A dedicated user per service, explicit modes on files, and root only through sudo."
sensei_tip: "Do not run Node as root to bind port 80: put nginx in front, or give the binary the CAP_NET_BIND_SERVICE capability. In Docker, the node images include a non-root user called node."
---

| JavaScript / TypeScript | Linux |
| --- | --- |
| fs.chmodSync(p, 0o755) | chmod 755 p |
| fs.statSync(p).mode | stat -c %a p |
| sudo npm install -g | npm prefix in your home, or apt |
| USER node in the Dockerfile | a service user such as www-data |
