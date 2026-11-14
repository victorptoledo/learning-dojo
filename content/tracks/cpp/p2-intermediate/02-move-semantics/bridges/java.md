---
from: java
status: draft
concept: "Move semantics"
from_way: "Every assignment just copies a reference to a heap object."
native_way: "Value types can steal the internal buffers of a temporary object (`std::move`), avoiding copies of large buffers."
sensei_tip: "`std::move` doesn't move anything: it only casts to an rvalue (`T&&`), enabling the move constructor."
---
