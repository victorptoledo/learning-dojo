---
from: python
status: draft
concept: "A single UI thread"
from_way: "Tkinter's mainloop: a long callback freezes the window; after() and threads keep it alive."
native_way: "The main thread is the browser's mainloop; long tasks block input and rendering, and workers run code in parallel."
sensei_tip: "after(0, step) in Tkinter is setTimeout(step, 0): the same trick for splitting work into chunks."
---

| Python | In the browser |
| --- | --- |
| `root.mainloop()` | The event loop |
| `root.after(0, step)` | `setTimeout(step, 0)` / `scheduler.yield()` |
| `threading.Thread` for heavy work | A Web Worker |
| `update_idletasks()` | A forced synchronous layout |
