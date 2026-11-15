---
from: javascript
status: draft
concept: "A missing value that is not a value"
from_way: "null and undefined, ?? and ?. operators, and loose versus strict equality."
native_way: "One NULL with three-valued logic, closer to NaN (NaN === NaN is false) than to null."
sensei_tip: "Think of NULL like NaN: it is not equal to anything, itself included. ?? is COALESCE with two arguments."
---

| JavaScript | SQL |
| --- | --- |
| x === null | x IS NULL |
| nick ?? name | coalesce(nick, name) |
| NaN === NaN // false | NULL = NULL -- UNKNOWN |
| Object.is(a, b) | a IS NOT DISTINCT FROM b |
| arr.filter(x => x != null).length | count(x) |
