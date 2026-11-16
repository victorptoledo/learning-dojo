---
from: go
status: draft
concept: "Deterministic rules, no magic"
from_way: "Go's explicit rules: no overloading, the closest declaration in scope wins."
native_way: "The cascade is explicit too, just with more steps: origin, layers, specificity, order. Learn them once and CSS stops surprising you."
sensei_tip: "Keep selectors flat like Go keeps types flat: one class per component part, no deep descendant chains."
---

| Go | In the browser |
| --- | --- |
| Shadowing by inner scope | A more specific selector |
| Package boundaries | `@layer` boundaries |
| Embedding a struct | Inheritance of text properties |
| `go doc` | MDN's property pages |
