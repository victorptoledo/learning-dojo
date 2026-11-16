---
from: csharp
status: draft
concept: "Operating what you ship"
from_way: "Application Insights, health checks, deployment slots and Azure App Configuration feature flags."
native_way: "Application Insights' JavaScript SDK, Web Vitals, static hosting with atomic deploys and client-side flags do the same for the frontend."
sensei_tip: "Deployment slots and swap are atomic deploys; Feature Management works in the browser too through a flags endpoint."
---

| C# | In the browser |
| --- | --- |
| Application Insights server SDK | Its JavaScript SDK in the page |
| Deployment slot swap | Switching index.html to the new hashed assets |
| `IFeatureManager` | Client-side feature flags |
| `CultureInfo` formatting | `Intl.NumberFormat` and `DateTimeFormat` |
