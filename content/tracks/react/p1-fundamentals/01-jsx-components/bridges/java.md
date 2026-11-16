---
from: java
status: draft
concept: "Composition of functions instead of widget classes"
from_way: "UI frameworks such as Swing or JavaFX build trees of widget objects through inheritance (extends JPanel) and constructors, and markup engines like Thymeleaf mix HTML with expressions."
native_way: "A component is a function from a props object to JSX; you compose them like method calls, pass content as children, and never inherit."
sensei_tip: "Treat props like the parameters of a record's canonical constructor: typed, immutable, all passed in. And treat the render function like a pure method: no fields changed, no I/O."
---

| Java | React |
| --- | --- |
| `class ProfilePanel extends JPanel` | `function Profile(props: ProfileProps)` |
| Constructor parameters | Props (one typed object) |
| `record AvatarProps(String name, int size)` | `type AvatarProps = { name: string; size?: number }` |
| `panel.add(child)` | `<Card>{child}</Card>` (children) |
| Thymeleaf `th:text="${name}"` | `{name}` |
| Pure methods are easy to test | Pure components are easy to test |
