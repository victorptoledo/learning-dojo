---
from: java
status: draft
concept: "Immutability by default"
from_way: "Everything is mutable unless marked `final`, and even then the referenced object can change."
native_way: "`let x = 5;` is immutable. To allow mutation you must say so explicitly: `let mut x = 5;`."
sensei_tip: "Shadowing lets you redeclare a name, e.g. `let x = \"42\";` then `let x: i32 = x.parse()?;`, without awkward names like `xStr`."
---
