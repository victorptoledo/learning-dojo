# React in Production: Architecture, Design Systems and Upgrades

Knowing hooks is not the same as running a React codebase that thirty people change every day.
This lesson collects the practices that keep a large React app healthy.

## Structure by feature

Small apps can group files by kind (`components/`, `hooks/`, `utils/`). Large apps group by
**feature**, so everything about one part of the product lives together and can be changed or
deleted as a unit:

```text
src/
  app/            routes, providers, layout
  features/
    cart/         CartPage.tsx, CartSummary.tsx, useCart.ts, cart.ts, cart.test.tsx, index.ts
    checkout/
    search/
  shared/
    ui/           the design system: Button, Dialog, TextField, Tabs
    lib/          API client, formatting, hooks used everywhere
```

Rules that help: features import from `shared/` and from other features' public `index.ts`
only, never from their internals (enforce it with lint rules); keep server-state hooks near the
feature; avoid a giant `utils/` with no owner.

## Component APIs

Treat components as APIs for other developers:

- small, typed props; boolean props for clear on/off variants, unions for choices
  (`variant: "primary" | "secondary"`);
- **composition** over configuration: `<Card><Card.Header />...</Card>` or `children` instead
  of twenty props;
- forward the native attributes (`...rest` to the underlying `<button>`), and accept `ref`
  (a plain prop in React 19);
- controlled and uncontrolled modes for inputs (`value`/`onChange` and `defaultValue`);
- accessible by default, so every consumer gets it right without trying.

## Design systems

A **design system** is the shared set of tokens (colours, spacing, typography), components and
guidelines that makes a product consistent. In code that usually means a `ui` package with
accessible primitives (often built on headless libraries such as Radix UI, React Aria or Base
UI), styled with CSS Modules, Tailwind or a CSS-in-JS solution that supports server rendering,
documented and tested in **Storybook**. Visual regression tests catch unintended changes.

## Guardrails

- **TypeScript strict** everywhere, with no `any` in props.
- **ESLint** with `eslint-plugin-react-hooks` (rules of hooks, exhaustive deps, React Compiler
  rules) and `eslint-plugin-jsx-a11y`.
- **Tests as a pyramid** (or "trophy"): many fast unit tests for pure logic (reducers,
  validators), integration tests with Testing Library for components and features, a few
  end-to-end tests with Playwright for the critical journeys (sign-up, checkout). Run them all in
  CI on every pull request.
- **Performance budgets**: track bundle size per route, and **Core Web Vitals** from real users:
  LCP (largest contentful paint), INP (interaction to next paint, where slow renders show up)
  and CLS (layout shift, often from loading states without skeletons).

## Production errors

- Wrap the app (and important regions) in **error boundaries** that show a useful fallback and
  report the error.
- Use an error monitoring service with **source maps**, release tags and user context, so a
  "white screen" becomes a stack trace.
- Log failed requests and unhandled promise rejections; handle errors in event handlers.

## Security pitfalls in React apps

- `dangerouslySetInnerHTML` with untrusted HTML is XSS: sanitise with a library such as
  DOMPurify, or render markdown to React elements.
- `href={userInput}` can be `javascript:...`: validate URLs (React 19 blocks `javascript:` URLs
  in development with a warning and in some cases at runtime, but do not rely on it).
- Anything in the client bundle is public: no secrets in client code or `VITE_`/`NEXT_PUBLIC_`
  variables.
- Server functions and loaders are endpoints: authenticate, authorise, validate.
- Store tokens in `HttpOnly` cookies rather than `localStorage` when possible.

## Upgrading React

React upgrades are usually incremental:

1. Upgrade to the latest minor of your current major and fix every warning in the console:
   warnings are the deprecations of the next major.
2. Run the official **codemods** (`npx codemod react/19/...`) for mechanical changes.
3. Upgrade React, React DOM and the types together; then libraries that depend on React
   internals (routers, state libraries, testing tools).
4. Turn on StrictMode where it is missing, fix what it reveals, ship behind a feature flag or to
   a share of users first.

From 17 to 19 the main steps are `createRoot` instead of `ReactDOM.render`, automatic batching,
StrictMode's double effects, removal of `propTypes` and `defaultProps` on function components,
string refs and legacy context, and `ref` as a prop instead of `forwardRef`.

## Key points

- Organise by feature with explicit public APIs; keep a shared, accessible design system.
- Guardrails: strict TypeScript, hook and a11y lint rules, a test pyramid in CI, performance
  budgets and Core Web Vitals.
- Error boundaries plus monitoring with source maps turn white screens into stack traces.
- Sanitise HTML, validate URLs, keep secrets on the server, treat server code as endpoints.
- Upgrade one step at a time, fixing warnings and running codemods.

## Further reading

- react.dev, "React 19 Upgrade Guide": <https://react.dev/blog/2024/04/25/react-19-upgrade-guide>
- react.dev, "Thinking in React": <https://react.dev/learn/thinking-in-react>
- react.dev, `dangerouslySetInnerHTML` and security notes: <https://react.dev/reference/react-dom/components/common#dangerously-setting-the-inner-html>
- web.dev, "Web Vitals": <https://web.dev/articles/vitals>
- Storybook, "Why Storybook?": <https://storybook.js.org/docs/get-started/why-storybook>
- Bulletproof React (architecture guide): <https://github.com/alan2207/bulletproof-react>
