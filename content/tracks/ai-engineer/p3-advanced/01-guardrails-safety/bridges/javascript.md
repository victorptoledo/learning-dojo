---
from: javascript
status: draft
concept: "Untrusted text that gives orders"
from_way: "XSS defences: never innerHTML untrusted strings, sanitise with DOMPurify, set a Content Security Policy."
native_way: "Model output is untrusted HTML and Markdown: sanitise it, restrict images and links, and never let it trigger actions without the user's confirmation."
sensei_tip: "A Content Security Policy with img-src limited to your own domains blocks the Markdown-image exfiltration trick even if an injection succeeds."
---

| JavaScript | LLM guardrail |
| --- | --- |
| DOMPurify | sanitising rendered model output |
| CSP `img-src 'self'` | blocking image-based exfiltration |
| `textContent` instead of `innerHTML` | showing output as text |
| a confirm dialog | approval before side effects |
| server-side session checks | authorisation in tools |
