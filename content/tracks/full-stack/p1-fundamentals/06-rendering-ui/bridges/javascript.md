---
from: javascript
status: reviewed
concept: "Not everything needs to be a SPA"
from_way: "Every screen is a client-rendered React page, including public pages that would be faster as plain HTML."
native_way: "Server-render or pre-render content pages, keep SPA routes for interactive tools, and treat forms, states and escaping the same way in both."
sensei_tip: "Map the API's problem details to form fields with a small helper once, and reuse it in every form."
---

| SPA habit | Rendering practice |
| --- | --- |
| a spinner on every page | server HTML for content pages |
| `alert("error")` | the message under the field, values kept |
| double-click submits twice | disable the button while pending |
| `dangerouslySetInnerHTML` | sanitise and review, or avoid |
