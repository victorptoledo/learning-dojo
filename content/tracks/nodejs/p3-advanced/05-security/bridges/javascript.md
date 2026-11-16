---
from: javascript
status: draft
concept: "From the browser sandbox to full access"
from_way: "The browser sandbox protects the machine; your security work is XSS, CSRF and CSP, and dependencies end up in a bundle."
native_way: "No sandbox: the process can read files, open sockets and start programs, so injection, path traversal and malicious dependencies hit the server itself."
sensei_tip: "An npm package in a front-end bundle can steal a page's data; the same package on a server can read every secret of the machine. Review server dependencies more strictly."
---

| Browser | Node.js server |
| --- | --- |
| Sandbox, same-origin policy | No sandbox (optional `--permission`) |
| XSS through `innerHTML` | Command, SQL and path injection |
| CSP | `--permission`, non-root user |
| Dependencies in a bundle | Dependencies with full system access |
| `crypto.getRandomValues` | `crypto.randomBytes` |
