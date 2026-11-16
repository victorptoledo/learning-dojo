# React 19 Projects: Vite, TSX and StrictMode

React is a JavaScript library for building user interfaces out of **components**: functions
that take data and return a description of the UI. You describe what the screen should look
like for the current state; when the state changes, React works out what changed and updates
the DOM for you. That one idea, **UI = f(state)**, is the whole track in four letters.

## What React is, and what it is not

React gives you components, state, hooks, rendering into the DOM (`react-dom`) and a few
building blocks for data loading (Suspense, `use`). It does **not** give you a router, an HTTP
client, a form library, a CSS system or a build tool. A project picks those, or uses a
**framework** built on React that bundles them: Next.js, React Router (framework mode) or Expo
for mobile. The React team recommends starting real apps with a framework; for learning, and
for apps that run fully in the browser, a plain **Vite** project is the simplest start.

| Concern | Who provides it |
| --- | --- |
| Components, state, hooks, rendering | React (`react`, `react-dom`) |
| Bundling, dev server, hot reload | Vite (or the framework) |
| Routing | React Router, TanStack Router, or the framework |
| Server data and caching | `fetch` plus a library such as TanStack Query, or the framework |
| Types | TypeScript and `@types/react` |

## A Vite + React + TypeScript project

```bash
npm create vite@latest my-app -- --template react-ts
cd my-app
npm install
npm run dev        # http://localhost:5173 with hot reload
```

The files that matter:

| File | Role |
| --- | --- |
| `index.html` | The one HTML page, with `<div id="root">` and a script tag for `src/main.tsx` |
| `src/main.tsx` | The entry point: creates the React root and renders `<App />` |
| `src/App.tsx` | Your first component |
| `tsconfig.json` | TypeScript settings, with `"jsx": "react-jsx"` |
| `vite.config.ts` | Vite with the React plugin |

`main.tsx` is short:

```tsx
import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { App } from "./App.tsx";

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <App />
  </StrictMode>,
);
```

`createRoot` attaches React to a DOM node; `render` shows a component tree inside it. From
then on React owns that node: you change the screen by changing state, never by editing the
DOM by hand.

## TSX

Components live in `.tsx` files: TypeScript plus **JSX**, the HTML-like syntax for UI. The
compiler turns each tag into a function call (`<App />` becomes `jsx(App, {})` from
`react/jsx-runtime`), so JSX is just JavaScript expressions. You will learn its rules in the
next phase; for now notice that a component is a function whose name starts with a capital
letter and that returns JSX:

```tsx
export function App() {
  return <h1>Hello, dojo!</h1>;
}
```

## StrictMode

`<StrictMode>` changes nothing in production. In development it helps you find bugs early:

- it renders each component **twice**, to catch render functions that are not pure;
- it runs every effect **mount → cleanup → mount** once more, to catch effects without a
  proper cleanup;
- it warns about deprecated APIs.

If something "happens twice" in development, StrictMode is showing you code that would
misbehave later (when a component remounts, or when React reuses state). Fix the code; keep
StrictMode on.

## The React Developer Tools

Install the **React Developer Tools** browser extension. The *Components* tab shows the tree
of components with their props, state and hooks; the *Profiler* tab records which components
rendered and why. You will use the Profiler in the performance lesson.

## How the dojo checks React katas

React katas are TypeScript projects with `.tsx` components and `.ts` hooks. The dojo checks
them in its `dojo-react` image, with no network:

1. `tsc` type-checks everything with strict settings and the React types. A type error
   means the code "does not compile", and no test runs.
2. `tsc` compiles the files to JavaScript, and the tests run with `node:test` in a
   **jsdom** document (a DOM written in JavaScript, no browser needed).
3. The tests use **Testing Library**: they render your component, find elements the way a
   user does (by role, label or text) and click or type with `user-event`.

So a kata test reads like a user story:

```tsx
render(<Counter />);
await user.click(screen.getByRole("button", { name: "Increment" }));
assert.ok(screen.getByText("Count: 1"));
```

Components and hooks that still throw `new Error("todo ...")` are yours to write. To run the
visible tests on your machine: `npm install` once, then `npm test`.

## Key points

- React renders UI from state; it is a library, and frameworks add routing, data and builds.
- A Vite project has one HTML page, `main.tsx` creates the root, components live in `.tsx`.
- StrictMode double-invokes renders and effects in development to expose bugs; keep it on.
- The React DevTools show the tree, props, state and why components rendered.

## Further reading

- react.dev, "Quick Start": <https://react.dev/learn>
- react.dev, "Creating a React App" (frameworks and build tools): <https://react.dev/learn/creating-a-react-app>
- react.dev, `<StrictMode>`: <https://react.dev/reference/react/StrictMode>
- react.dev, `createRoot`: <https://react.dev/reference/react-dom/client/createRoot>
- Vite guide, "Getting Started": <https://vite.dev/guide/>
- roadmap.sh, React roadmap: <https://roadmap.sh/react>
