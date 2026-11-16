---
from: java
status: draft
concept: "Accessibility as an interface contract"
from_way: "Swing's AccessibleContext or JavaFX's accessible properties give each widget a role, name and state for screen readers."
native_way: "The browser derives role, name and state from HTML into the accessibility tree; ARIA attributes override them when HTML cannot say it."
sensei_tip: "If you ever set getAccessibleName in Swing, aria-label is the same idea, and the native element is the component that already implements the interface."
---

| Java | In the browser |
| --- | --- |
| `AccessibleRole.PUSH_BUTTON` | `<button>` (role button) |
| `setAccessibleName("Close")` | `aria-label="Close"` |
| `setLabelFor(field)` | `<label for="id">` |
| Focus traversal policy | DOM order and `tabindex` |
