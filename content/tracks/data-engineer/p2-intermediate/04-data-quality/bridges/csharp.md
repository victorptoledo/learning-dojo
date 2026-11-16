---
from: csharp
status: draft
concept: "Testing data, not only code"
from_way: "FluentValidation for requests and xUnit for services."
native_way: "Dataset checks with severities that block or warn before data is published."
sensei_tip: "FluentValidation rules are row-level checks; for batch rules, a SQL query returning failing rows run by the pipeline (or tSQLt tests in SQL Server) does the job."
---

| C# | Data quality |
| --- | --- |
| FluentValidation RuleFor | row-level validity |
| xUnit with small inputs | unit tests of transformations |
| tSQLt | tests for SQL code in SQL Server |
| a CHECK constraint | validity enforced on write |
| a failed build on schema change | a contract in the producer's CI |
