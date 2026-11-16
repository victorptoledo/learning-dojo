---
from: java
status: draft
concept: "Listeners as props, lists as streams"
from_way: "Swing registers listeners with addActionListener; lists are rendered by adapters or cell factories that recycle rows by position."
native_way: "Handlers are functions passed as props; lists are arrays mapped to elements, and keys tell React which row is which."
sensei_tip: "A key is the equals/hashCode of a row: React uses it to match old and new items. An index as key is like hashing by position."
---

| Java | React |
| --- | --- |
| `button.addActionListener(e -> save())` | `<button onClick={save}>` |
| Custom listener interface | `onDelete: () => void` prop |
| `list.stream().filter(...).map(...)` | `items.filter(...).map(...)` |
| `equals`/`hashCode` identity | `key={item.id}` |
| `Optional.ifPresent` | `{user && <p>{user.name}</p>}` |
| `event.consume()` | `event.stopPropagation()` |
