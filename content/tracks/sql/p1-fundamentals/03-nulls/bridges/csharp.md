---
from: csharp
status: draft
concept: "A missing value that is not a value"
from_way: "Nullable reference and value types, the ?? and ?. operators, and lifted operators on int?."
native_way: "NULL with three-valued logic; C#'s bool? with & and | follows the same truth tables."
sensei_tip: "C#'s lifted operators already behave like SQL: (int?)null + 1 is null, and bool? null & false is false. EF Core translates == null to IS NULL for you."
---

| C# | SQL |
| --- | --- |
| x is null | x IS NULL |
| nick ?? name | coalesce(nick, name) |
| int? n = null; n + 1 == null | NULL + 1 is NULL |
| bool? b = null; b & false == false | UNKNOWN AND FALSE is FALSE |
| Equals(a, b) | a IS NOT DISTINCT FROM b |
