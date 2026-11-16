---
from: go
status: draft
concept: "Components and architecture"
from_way: "templ or html/template partials rendered on the server."
native_way: "Server-rendered HTML with custom elements as islands: the server stays in charge and small scripts enhance parts of the page."
sensei_tip: "Go's strength is fast server rendering: pair it with web components and you get SSR plus interactivity without a JavaScript framework."
---

| Go | In the browser |
| --- | --- |
| A template partial | A custom element with a template |
| Interfaces as contracts | Attributes and events as a component's API |
| Server-side rendering with templ | SSR with progressive enhancement |
| Packages with small APIs | Feature modules with explicit exports |
