---
from: javascript
status: draft
concept: "Redux without the library"
from_way: "You may know Redux reducers and actions, or pass shared objects through module imports and event emitters."
native_way: "useReducer is a local Redux: same reducer and actions; context distributes state and dispatch to the subtree."
sensei_tip: "If you know Redux, you know useReducer. What is missing is selectors: context re-renders every consumer, so split it or move to a store when that hurts."
---

| JavaScript | React |
| --- | --- |
| Redux `createStore(reducer)` | `useReducer(reducer, initial)` |
| `store.dispatch({ type: "add" })` | `dispatch({ type: "add" })` |
| `<Provider store>` | `<CartContext value={...}>` |
| `useSelector(s => s.items)` | `useContext(CartContext).state.items` (no selector) |
| Module singleton for shared state | Context, scoped to a subtree |
| String action types | Discriminated union, checked by TypeScript |
