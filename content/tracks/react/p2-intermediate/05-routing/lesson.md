# Client-Side Routing with React Router

A single-page app (SPA) loads one HTML page, then changes what it shows when the URL changes,
without asking the server for a new page. A **router** maps URLs to components, keeps the
browser's history (back and forward work), and turns links into in-app navigation. React has
no router of its own; **React Router** is the most used one, and frameworks (Next.js, React
Router framework mode, TanStack Router) build on the same ideas.

## Routes, layouts and outlets

Describe the app as a tree of routes. A parent route renders a **layout** and an `<Outlet />`
where the matching child appears:

```tsx
import { createBrowserRouter, RouterProvider, Outlet, Link } from "react-router";

const router = createBrowserRouter([
  {
    path: "/",
    element: <RootLayout />,
    errorElement: <ErrorPage />,
    children: [
      { index: true, element: <Home /> },                     // "/"
      { path: "products", element: <ProductList /> },          // "/products"
      { path: "products/:productId", element: <ProductPage /> },
      {
        path: "account",
        element: <AccountLayout />,                             // tabs + <Outlet />
        children: [
          { path: "orders", element: <Orders /> },
          { path: "settings", element: <Settings /> },
        ],
      },
      { path: "*", element: <NotFound /> },
    ],
  },
]);

function RootLayout() {
  return (
    <>
      <nav>
        <Link to="/">Home</Link> <Link to="/products">Products</Link>
      </nav>
      <main><Outlet /></main>
    </>
  );
}

createRoot(root).render(<RouterProvider router={router} />);
```

Nested routes match nested UI: the header stays mounted while the page under it changes, and
each layout keeps its state.

## Links and navigation

- `<Link to="/products">` renders an `<a href>` that navigates without a page reload.
  `<NavLink>` adds an active state (`aria-current="page"`) for menus.
- `useNavigate()` returns a function for navigating from code, for example after a save:
  `navigate("/orders")`. Use links for anything the user clicks, so middle-click, "open in
  new tab" and screen readers work.

## The URL is state

The URL is the one piece of state the user can bookmark, share and reload. Put there what
identifies the view:

```tsx
import { useParams, useSearchParams } from "react-router";

function ProductPage() {
  const { productId } = useParams();               // "/products/42" → "42"
  // ...
}

function ProductList() {
  const [params, setParams] = useSearchParams();   // "/products?category=books&page=2"
  const category = params.get("category") ?? "all";
  const page = Number(params.get("page") ?? "1");
  return (
    <select
      value={category}
      onChange={(event) => setParams({ category: event.target.value, page: "1" })}
    >
      ...
    </select>
  );
}
```

Path params identify a resource (`/products/:productId`); **search params** hold filters,
sorting, pagination and selected tabs. Everything else (an open dropdown, the text being
typed) stays in React state. Params are strings or `undefined`: parse and validate them.

## Loaders, actions and lazy routes

With a data router, a route can declare a **loader** that fetches its data when the route
matches, in parallel for all matched routes, before the component renders. Components read it
with `useLoaderData()`. An **action** handles a form submitted to that route, then the router
reloads the data. Errors thrown in loaders show the route's `errorElement`. This removes the
fetch-after-render waterfalls of effect-based fetching.

`lazy` routes load their component code only when visited (**code splitting**), so the first
page downloads less JavaScript. In framework mode, React Router and Next.js do this per route
automatically.

## Protecting routes

A route that needs a signed-in user checks it before rendering, typically in its loader
(`if (!user) throw redirect("/login?next=/checkout")`) or in a wrapper layout that renders
`<Navigate to="/login" replace />`. This is a **user-experience** check only: the server must
still authorise every request, because client code can be changed by anyone.

## Hosting an SPA

A client-side router needs the server to answer every app URL (`/products/42`) with
`index.html`; otherwise a reload gives a 404. Static hosts call this a "SPA fallback" or a
rewrite rule. Frameworks with server rendering handle it themselves.

## Key points

- A router maps URLs to nested routes; layouts render an `<Outlet />` for their children.
- Use `<Link>`/`<NavLink>` for navigation, `useNavigate` for navigation from code.
- Keep shareable view state in the URL: path params for resources, search params for filters.
- Loaders fetch per route before render, actions handle submissions, lazy routes split code.
- Route guards improve UX; the server must still authorise.

## Further reading

- React Router, "Routing" (framework and data modes): <https://reactrouter.com/start/framework/routing>
- React Router, "Picking a Mode": <https://reactrouter.com/start/modes>
- React Router, `useSearchParams`: <https://reactrouter.com/api/hooks/useSearchParams>
- react.dev, "Creating a React App" (frameworks with routing): <https://react.dev/learn/creating-a-react-app>
- MDN, History API: <https://developer.mozilla.org/en-US/docs/Web/API/History_API>
