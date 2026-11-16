---
from: csharp
status: draft
concept: "State and persistence"
from_way: "ASP.NET Core session, TempData, IMemoryCache and Entity Framework."
native_way: "In the browser: memory, the URL, Web Storage, IndexedDB and cookies, each with its own lifetime and size."
sensei_tip: "Blazor's ProtectedLocalStorage exists because localStorage is readable by any script: in plain JavaScript, simply never put secrets there."
---

| C# | In the browser |
| --- | --- |
| `HttpContext.Session` | `sessionStorage` or an HttpOnly cookie |
| `IMemoryCache` | A `Map` in memory |
| SQLite with EF Core | IndexedDB |
| `[FromQuery]` filters | URL query parameters |
