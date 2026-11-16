---
from: javascript
status: draft
concept: "Ask what can go wrong before writing the code"
from_way: "Shipping features fast and adding security fixes when bug reports come in."
native_way: "A one-hour threat modeling session for features with new data, new third parties or new trust boundaries, and mitigations tracked with the feature's tickets."
sensei_tip: "For front-end heavy features, put the browser explicitly outside the trust boundary in the DFD; it makes it obvious that every check in client code must exist on the server too."
---

| JavaScript / TypeScript | Threat modeling habit |
| --- | --- |
| client-side checks as the control | the browser is outside the boundary |
| third-party scripts added freely | each one is a new element and threat |
| no abuse cases | tests for misuse |
| security as a final gate | part of design review |
| threats discussed, not written | a threat list with decisions |
