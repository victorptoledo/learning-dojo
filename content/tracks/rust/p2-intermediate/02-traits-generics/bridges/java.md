---
from: java
status: draft
concept: "Interfaces vs traits"
from_way: "`interface Flyable { void fly(); }` must be declared with `implements` when the class is written."
native_way: "`trait Flyable { fn fly(&self); }` can be implemented for any type, including standard library types you didn't write (within the orphan rule)."
sensei_tip: "Generic trait bounds use static dispatch (monomorphisation) with no runtime cost; dynamic dispatch happens only when you ask for `dyn Trait`."
---
