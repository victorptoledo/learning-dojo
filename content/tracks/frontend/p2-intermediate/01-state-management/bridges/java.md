---
from: java
status: draft
concept: "Interface state"
from_way: "Session state on the server or values stored in global variables."
native_way: "The UI is a pure function of state: UI = f(state). Server data should be cached with automatic invalidation."
sensei_tip: "Stop storing API data in a global store: let a server-state library like TanStack Query handle caching, refetching and loading."
---
