---
from: python
status: draft
concept: "Lookup order like the MRO"
from_way: "Attribute lookup follows the method resolution order (instance, class, bases)."
native_way: "Property values come from the cascade first; if nothing sets an inheritable property, it comes from the parent element."
sensei_tip: "Specificity is CSS's MRO: deterministic, inspectable (DevTools shows the struck-out losers), and best kept shallow."
---

| Python | In the browser |
| --- | --- |
| MRO | Origin → layers → specificity → order |
| Instance attribute shadows class attribute | A more specific rule shadows a general one |
| Inherited attribute from a base class | Inherited property from a parent element |
| `vars(obj)` | DevTools' Computed panel |
