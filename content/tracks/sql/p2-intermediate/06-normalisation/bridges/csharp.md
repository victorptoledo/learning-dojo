---
from: csharp
status: draft
concept: "Storing each fact once"
from_way: "Entity classes with navigation properties, owned types and collections in EF Core."
native_way: "Normalised tables, with join tables for many-to-many."
sensei_tip: "EF Core creates a join table for skip navigations; when the relationship needs a date or a role, make that join entity explicit."
---

| C# | SQL |
| --- | --- |
| ICollection<Course> Courses on Student | enrolments junction table |
| an owned type Address | columns in the same table (fine when it is one-to-one) |
| a string of comma-separated ids | a 1NF violation |
| Price copied into OrderLine | history, not redundancy |
