---
from: javascript
status: reviewed
concept: "The platform under the framework"
from_way: "React or Vue developers rarely write a plain form or a fetch call without a library around it."
native_way: "Native forms, the DOM and fetch are what every framework compiles to; knowing them explains framework behaviour and failures."
sensei_tip: "Try your form with JavaScript disabled once: if it still submits, you built it on the platform instead of around it."
---

| Framework habit | Platform |
| --- | --- |
| `onClick` on a `div` | a `<button>` in a `<form>`, keyboard accessible |
| axios throws on 4xx | `fetch` does not: check `response.ok` |
| JSX escapes `{text}` | `textContent`; `dangerouslySetInnerHTML` is `innerHTML` |
| client-side router | a GET form gives a shareable search URL for free |
