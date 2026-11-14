---
from: java
status: draft
concept: "Lifecycle vs synchronisation"
from_way: "Thinking in imperative class methods like `componentDidMount()` and `componentDidUpdate()`."
native_way: "Function components run on every render; `useEffect` synchronises the component with an external system (an API, a timer)."
sensei_tip: "If you use `useEffect` to compute data from other state, stop: compute it during render, or with `useMemo` if it is expensive."
---
