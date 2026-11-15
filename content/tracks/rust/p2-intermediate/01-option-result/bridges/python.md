---
from: python
status: draft
concept: "Null safety and errors"
from_way: "None for absence and exceptions for errors, checked at runtime."
native_way: "Option<T> for absence and Result<T, E> for errors, both part of the type and propagated with ?."
sensei_tip: "Every Python function might raise; a Rust function that can fail says so in its signature. ? is like letting an exception propagate, but visible at each call."
---

| Python | Rust |
| --- | --- |
| return None | None (Option<T>) |
| raise ValueError | Err(MyError::Invalid) |
| try/except | match or ? |
| x or default | x.unwrap_or(default) |
| if x is not None: | if let Some(x) = x |
