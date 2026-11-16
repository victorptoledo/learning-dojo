---
from: csharp
status: draft
concept: "Knowing where data comes from and who may see it"
from_way: "SQL Server permissions per database and documentation in a wiki."
native_way: "Microsoft Purview (or Unity Catalog) for lineage and classification, with row-level security and dynamic data masking."
sensei_tip: "SQL Server and Azure SQL have row-level security (security policies) and dynamic data masking built in, and Purview scans sources to classify PII automatically."
---

| C# | Lineage and governance |
| --- | --- |
| GRANT SELECT ON SCHEMA | role-based access |
| Dynamic Data Masking | column masks |
| CREATE SECURITY POLICY | row-level security |
| Microsoft Purview | catalog, lineage, classification |
| Always Encrypted | column encryption |
