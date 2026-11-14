---
from: java
status: draft
concept: "Null safety and errors"
from_way: "`null` causing `NullPointerException`, plus try/catch for checked and unchecked exceptions."
native_way: "There is no `null`: there is `Option<T>` (`Some` or `None`). There are no exceptions: errors are values, `Result<T, E>` (`Ok` or `Err`)."
sensei_tip: "The `?` operator propagates an error to the caller in one character, much cleaner than nested try/catch blocks."
---
