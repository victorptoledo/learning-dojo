---
from: csharp
status: draft
concept: "Like Blazor components, but in the browser's JavaScript"
from_way: "Blazor or Razor components combine markup and C# and re-render when state changes (StateHasChanged); WPF binds XAML to view models."
native_way: "React components are TypeScript functions returning JSX; calling a state setter schedules a re-render, with no binding framework."
sensei_tip: "If you know Blazor, React will feel familiar: components, parameters (props) and re-rendering on state change. The difference is that React components are plain functions, and state lives in hooks."
---

| C# / .NET | React |
| --- | --- |
| `dotnet new blazorwasm` | `npm create vite@latest -- --template react-ts` |
| `.razor` file with markup and `@code` | `.tsx` file with a function returning JSX |
| `StateHasChanged()` | Calling a state setter (`setCount`) |
| WPF data binding | State and props passed down explicitly |
| `Program.cs` builds the host | `main.tsx` creates the root |
| Debug-only checks | StrictMode in development |
