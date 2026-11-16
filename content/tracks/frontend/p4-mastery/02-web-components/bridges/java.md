---
from: java
status: draft
concept: "Components and architecture"
from_way: "JSF or Vaadin components, modules with clear APIs (JPMS), and choosing between server-rendered pages and rich clients."
native_way: "Custom elements are the platform's components; rendering strategies are the server-versus-client choice made per page."
sensei_tip: "Vaadin's web components are built on these exact standards: a Java developer has likely used custom elements without knowing it."
---

| Java | In the browser |
| --- | --- |
| A JSF or Vaadin component | A custom element |
| Component lifecycle (onAttach / onDetach) | `connectedCallback` / `disconnectedCallback` |
| JPMS module boundaries | Feature modules with explicit exports |
| Server-rendered JSP vs a rich client | SSR/SSG vs CSR |
