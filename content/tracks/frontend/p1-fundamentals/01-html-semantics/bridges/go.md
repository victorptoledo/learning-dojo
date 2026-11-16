---
from: go
status: draft
concept: "Meaning before presentation"
from_way: "`html/template` writes whatever markup you give it, escaped."
native_way: "The browser and assistive technology interpret element names: the template should output the most meaningful element, not just a styled div."
sensei_tip: "Go favors explicit, boring code; semantic HTML is the same virtue: say what the thing is."
---

| Go | In the browser |
| --- | --- |
| A struct with named fields | Semantic elements |
| `time.Time` formatted | `<time datetime>` |
| `{{range}}` over a slice | `<ul>` of `<li>` |
| Comments on exported types | `alt`, captions and labels |
