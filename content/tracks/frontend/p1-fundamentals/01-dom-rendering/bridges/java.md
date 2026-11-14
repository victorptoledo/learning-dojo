---
from: java
status: draft
concept: "Browser rendering"
from_way: "Server-rendered pages that reload completely on every click."
native_way: "The browser builds the DOM and CSSOM trees in memory; changing classes can trigger reflow and repaint, which cost CPU and GPU."
sensei_tip: "For 60 fps animations, change only properties the compositor handles without reflow: `transform` and `opacity`."
---
