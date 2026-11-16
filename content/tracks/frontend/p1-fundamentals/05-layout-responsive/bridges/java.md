---
from: java
status: draft
concept: "Layout managers"
from_way: "Swing's BoxLayout, GridBagLayout and JavaFX's HBox, VBox and GridPane."
native_way: "Flexbox is HBox/VBox with wrapping and growth factors; grid is GridPane with fractions, named areas and auto-placement."
sensei_tip: "If GridBagLayout ever hurt you, grid is the cure: rows, columns, gaps and spans in a few declarations."
---

| Java | In the browser |
| --- | --- |
| `HBox` / `VBox` | `display: flex` with row or column |
| `HBox.setHgrow(node, ALWAYS)` | `flex: 1` |
| `GridPane` with constraints | `display: grid` with `grid-template-columns` |
| Resizing listeners | Media and container queries |
