---
from: python
status: draft
concept: "A missing value that is not a value"
from_way: "None, the is None test, and the falsy behaviour of None in if statements."
native_way: "NULL with three-valued logic: NULL = NULL is not TRUE, and NOT of UNKNOWN is still UNKNOWN."
sensei_tip: "None == None is True in Python; NULL = NULL is UNKNOWN in SQL. Always write IS NULL, never = NULL."
---

| Python | SQL |
| --- | --- |
| x is None | x IS NULL |
| a == b (None == None is True) | a IS NOT DISTINCT FROM b |
| nick or name | coalesce(nick, name) |
| len([x for x in xs if x is not None]) | count(x) |
| x / y if y else None | x / nullif(y, 0) |
