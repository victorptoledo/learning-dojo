---
from: java
status: draft
concept: "Diffing and list keys"
from_way: "Updating the interface by manipulating DOM nodes directly."
native_way: "React compares two virtual trees in memory and sends only the minimal changes to the browser."
sensei_tip: "Never use the array index as `key` when the list can be reordered, filtered or deleted from: it confuses reconciliation and causes visual bugs."
---
