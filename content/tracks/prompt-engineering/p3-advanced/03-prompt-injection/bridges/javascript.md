---
from: javascript
status: draft
concept: "XSS thinking for prompts"
from_way: "Escaping user content before inserting it into HTML, Content Security Policy, and never using innerHTML with untrusted strings."
native_way: "Untrusted text is escaped and delimited before it enters a prompt, and the model's output is checked like untrusted HTML before it is rendered or acted on."
sensei_tip: "Rendering model output as Markdown is like innerHTML: an image tag pointing to an attacker's domain leaks data the moment it loads. Allow-list image and link domains, or render as plain text."
---

| JavaScript | Prompt-injection concept |
| --- | --- |
| escaping before `innerHTML` | escaping untrusted text in the prompt |
| `textContent` instead of `innerHTML` | treating content as data only |
| Content Security Policy `img-src` | allow-listing output link domains |
| `new URL(href).hostname` | checking a link's host |
| a confirm dialog before a destructive action | human confirmation |
