---
from: csharp
status: draft
concept: "@page directives and NavigationManager"
from_way: "Blazor components declare @page \"/products/{Id}\", read [Parameter] and [SupplyParameterFromQuery], and navigate with NavigationManager; layouts use @Body."
native_way: "React Router declares routes in one tree, reads useParams and useSearchParams, navigates with useNavigate, and layouts render <Outlet />."
sensei_tip: "@Body is <Outlet />, NavigationManager.NavigateTo is navigate(), [SupplyParameterFromQuery] is useSearchParams. Routes live in one tree instead of on each component."
---

| Blazor | React Router |
| --- | --- |
| `@page "/products/{Id}"` | `{ path: "products/:productId" }` |
| `[Parameter] string Id` | `useParams().productId` |
| `[SupplyParameterFromQuery]` | `useSearchParams()` |
| `@layout MainLayout` + `@Body` | Layout route + `<Outlet />` |
| `NavigationManager.NavigateTo` | `useNavigate()` |
| `<AuthorizeView>` | A guard route; the server still authorises |
