---
from: csharp
status: draft
concept: "Patterns after lambdas, records and sealed types"
from_way: "Delegates, events, records, and LINQ-friendly designs."
native_way: "The same modern style in Java: lambdas, records and sealed types."
sensei_tip: "Most C# and Java pattern implementations translate directly. The visitor is where Java now shines: a sealed interface plus a switch replaces the double dispatch."
---

| C# | Java 25 |
| --- | --- |
| Func<T> strategy | Function/Supplier strategy |
| event handlers | listeners or Consumer |
| records with with | records + withers |
| visitor with double dispatch | sealed interface + switch |
