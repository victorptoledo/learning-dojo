---
from: csharp
status: draft
concept: "A single UI thread"
from_way: "WPF's Dispatcher thread with its measure, arrange and render passes; Task.Run moves work off it."
native_way: "The browser has the same split: layout (measure/arrange) and paint on the main thread, workers for background work."
sensei_tip: "InvalidateMeasure is a style or geometry write; reading ActualWidth after it forces layout, exactly like offsetHeight."
---

| C# | In the browser |
| --- | --- |
| Dispatcher thread | The main thread |
| Measure and arrange passes | Layout |
| `await Task.Run(...)` | A Web Worker |
| `RenderTransform` animations | `transform` animations on the compositor |
