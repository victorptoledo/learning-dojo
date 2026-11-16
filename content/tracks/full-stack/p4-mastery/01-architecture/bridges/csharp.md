---
from: csharp
status: reviewed
concept: "Modular monoliths and BFFs in .NET"
from_way: "One project per layer (Web, Business, Data) shared by every feature."
native_way: "One project per module with internal types, architecture tests, YARP as a gateway or BFF, and .NET Aspire to run the pieces together locally."
sensei_tip: "Mark module internals as internal and expose a small public interface; the compiler then enforces most of your boundaries."
---

| .NET | Architecture |
| --- | --- |
| one project per module, `internal` types | enforced boundaries |
| NetArchTest / ArchUnitNET | architecture tests |
| YARP | a gateway or BFF |
| a solution with many projects | a .NET monorepo |
