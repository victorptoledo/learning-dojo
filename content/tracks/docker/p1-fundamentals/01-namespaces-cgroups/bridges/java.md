---
from: java
status: draft
concept: "Kernel-level isolation"
from_way: "Thinking Docker emulates hardware or runs a full operating system."
native_way: "A container is a normal Linux process isolated by namespaces (network, PID, filesystem) and limited by cgroups (CPU, memory)."
sensei_tip: "Because containers share the host kernel, their CPU overhead is close to zero."
---
