---
from: csharp
status: draft
concept: "Markup as a typed model"
from_way: "Strongly typed view models in Razor; tag helpers that generate correct markup."
native_way: "Semantic HTML elements are the strongly typed layer of the page: their meaning is part of the platform's contract with assistive technology."
sensei_tip: "Razor tag helpers like asp-for already generate label/for pairs: keep that habit for every element you write by hand."
---

| C# | In the browser |
| --- | --- |
| A view model with typed properties | Semantic elements |
| `DateTime` in the model | `<time datetime>` |
| `<label asp-for>` | `<label for>` |
| XML doc comments | `alt`, `<caption>`, `<legend>` |
