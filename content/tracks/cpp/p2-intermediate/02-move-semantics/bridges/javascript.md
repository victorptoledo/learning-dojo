---
from: javascript
status: draft
concept: "Moving instead of copying"
from_way: "Objects and arrays are shared by reference; structuredClone copies explicitly."
native_way: "Objects are copied by default; moves transfer their heap buffers without copying and leave the source empty."
sensei_tip: "Transferring an ArrayBuffer to a worker detaches it from the sender: that is a move. C++ does that for any type with a move constructor."
---

| JavaScript / TypeScript | C++ |
| --- | --- |
| `const b = a` (shared) | `auto b = std::move(a);` (transfer) |
| `structuredClone(a)` | `auto b = a;` (copy) |
| `postMessage(buf, [buf])` (transfer) | `std::move(buf)` |
| returning objects is free | returning values is free (elision or move) |
| detached buffer after transfer | moved-from object: valid, usually empty |
