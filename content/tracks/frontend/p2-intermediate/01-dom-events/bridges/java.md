---
from: java
status: draft
concept: "Events and listeners"
from_way: "Swing's ActionListener on each JButton, or JavaFX's setOnAction and event filters and handlers."
native_way: "addEventListener on DOM nodes; events capture down and bubble up the tree, so one listener on a parent can serve many children."
sensei_tip: "JavaFX already has capturing filters and bubbling handlers: the DOM is the same model, and delegation is a handler on the parent node."
---

| Java | In the browser |
| --- | --- |
| `button.addActionListener(l)` | `button.addEventListener("click", l)` |
| JavaFX `addEventFilter` | `addEventListener(type, l, { capture: true })` |
| `event.consume()` | `event.stopPropagation()` |
| `new JLabel(text)` | `createElement` + `textContent` |
