---
from: csharp
status: draft
concept: "Where transformation runs"
from_way: "SSIS data flows with lookups and derived columns, transforming rows before the destination."
native_way: "Load raw tables first, then transform with SQL models in the warehouse."
sensei_tip: "SSIS is classic ETL. The ELT equivalent in the Microsoft world is loading raw files into a lakehouse or Synapse and transforming with SQL or notebooks."
---

| C# | ETL / ELT |
| --- | --- |
| SSIS source component | extract |
| Derived Column, Lookup | the T of ETL |
| OLE DB destination | load |
| staging tables then stored procedures | ELT |
| database projects (.sqlproj) | versioned SQL models |
