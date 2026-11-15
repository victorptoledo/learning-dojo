---
from: csharp
status: draft
concept: "Structuring a Spring application"
from_way: "Projects per layer or per feature, and modular monoliths with internal types."
native_way: "Packages per module, events, and Spring Modulith checks."
sensei_tip: "C# can enforce boundaries with internal and separate assemblies; Spring Modulith achieves the same inside one artifact by convention plus tests."
---

| C# | Spring Boot 4 |
| --- | --- |
| one assembly per module | a package per module |
| internal | internal subpackages |
| MediatR notifications | application events |
| ArchUnitNET | ArchUnit / Spring Modulith |
