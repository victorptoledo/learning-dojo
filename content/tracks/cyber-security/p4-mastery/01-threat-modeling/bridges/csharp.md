---
from: csharp
status: draft
concept: "Ask what can go wrong before writing the code"
from_way: "Diagrams in Visio with no security view, and the Microsoft Threat Modeling Tool tried once."
native_way: "STRIDE (which Microsoft created) applied per element to a current DFD, integrated with the team's work items and revisited per release."
sensei_tip: "Microsoft's SDL puts threat modeling in the design phase for a reason; the Threat Modeling Tool's templates for Azure services give a quick start, but the conversation is what finds the threats."
---

| C# | Threat modeling habit |
| --- | --- |
| Visio boxes without boundaries | a DFD with trust boundaries |
| a tool report nobody reads | threats as work items |
| STRIDE as a checklist only | STRIDE per element and flow |
| authorization tested manually | integration tests per policy |
| one model per product, years old | per feature, kept current |
