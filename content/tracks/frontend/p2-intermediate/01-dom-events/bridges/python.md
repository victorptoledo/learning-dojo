---
from: python
status: draft
concept: "Events and callbacks"
from_way: "Tkinter's widget.bind(\"<Button-1>\", handler) or Qt's signals and slots."
native_way: "Listeners on DOM nodes receive an event object that travels from the target up through its ancestors."
sensei_tip: "bind_all and bind on a parent frame are delegation in Tkinter; in the browser it is the default way to handle lists."
---

| Python | In the browser |
| --- | --- |
| `widget.bind("<Button-1>", f)` | `el.addEventListener("click", f)` |
| `event.widget` | `event.target` |
| `return "break"` | `event.stopPropagation()` / `preventDefault()` |
| `Label(text=...)` | `createElement` + `textContent` |
