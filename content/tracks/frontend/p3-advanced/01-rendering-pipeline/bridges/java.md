---
from: java
status: draft
concept: "A single UI thread"
from_way: "Swing's Event Dispatch Thread and JavaFX's Application Thread: long work there freezes the UI; SwingWorker moves it off."
native_way: "The browser's main thread runs scripts, style, layout and paint; Web Workers are the background threads."
sensei_tip: "Everything you learned about not blocking the EDT applies: yield or use a worker, and only touch the DOM from the main thread."
---

| Java | In the browser |
| --- | --- |
| Event Dispatch Thread | The main thread |
| `SwingWorker` / `Task` | A Web Worker with `postMessage` |
| `SwingUtilities.invokeLater` | `requestAnimationFrame` / `setTimeout` |
| `revalidate()` then `repaint()` | Layout then paint |
