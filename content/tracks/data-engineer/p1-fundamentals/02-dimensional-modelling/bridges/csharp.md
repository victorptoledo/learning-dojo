---
from: csharp
status: draft
concept: "Modelling for reading, not writing"
from_way: "Normalised EF Core entities designed for transactional updates in SQL Server."
native_way: "Star schemas as used by SSAS and Power BI: facts with measures, dimensions with attributes."
sensei_tip: "Power BI's modelling guidance is Kimball's: star schemas, single-direction relationships from dimensions to facts, and measures (DAX) defined on additive columns."
---

| C# | Dimensional modelling |
| --- | --- |
| EF Core entities in 3NF | the source (OLTP) model |
| an IDENTITY column | a surrogate key |
| a Power BI fact table | fact_sales |
| a Power BI date table | dim_date |
| a DAX measure | an aggregate over an additive fact |
