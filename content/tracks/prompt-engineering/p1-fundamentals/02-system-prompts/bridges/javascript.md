---
from: javascript
status: draft
concept: "Server-side rules, client-side input"
from_way: "Validation in the browser for convenience, with the real authorisation on the server, and request bodies built from form data."
native_way: "The system prompt is the trusted, operator-side instruction; the user turn carries untrusted input; real permissions live in the code that executes actions."
sensei_tip: "Treat the instruction hierarchy like client-side validation: helpful, usually respected, never the only check."
---

| JavaScript | Prompt concept |
| --- | --- |
| server config, environment variables | the system prompt |
| `req.body` | the user turn |
| client-side form validation | rules stated in the prompt |
| server-side authorisation middleware | enforcement in the tool's code |
| `messages.slice(-20)` | trimming conversation history |
