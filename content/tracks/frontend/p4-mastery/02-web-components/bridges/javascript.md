---
from: javascript
status: draft
concept: "Components without a framework"
from_way: "React, Vue or Svelte components with props, state and a virtual DOM or compiler."
native_way: "Custom elements with attributes and properties, events instead of callbacks, and shadow DOM for encapsulation; usable inside any framework."
sensei_tip: "Props are attributes or properties, callbacks are CustomEvents, children are slots: map your component vocabulary and you can ship to every framework at once."
---

| JavaScript / Node.js | In the browser |
| --- | --- |
| Props | Attributes and properties |
| `onChange` callback prop | `dispatchEvent(new CustomEvent("change"))` |
| `children` / named slots | `<slot>` / `<slot name>` |
| CSS modules | Shadow DOM styles |
