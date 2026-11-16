# State Management at Scale: Client vs Server State

"Which state management library should we use?" is usually the wrong first question. The
right one is: **what kinds of state does this app have, and where does each kind belong?**
Most apps need far less global client state than they think.

## The kinds of state

| Kind | Examples | Best home |
| --- | --- | --- |
| **Local UI state** | a dropdown is open, the hovered row, the text being typed | `useState` in the component |
| **Shared client state** | the signed-in user, theme, a multi-step wizard, a cart | lifted state, context + reducer, or a store |
| **Server state** | products, orders, the user's profile from the API | a query cache (TanStack Query, SWR, RTK Query) or framework loaders |
| **URL state** | the current page, filters, the selected tab, the product id | the router: path and search params |
| **Form state** | values, touched fields, errors of a form being edited | local state or a form library |

Server state is the big one. It is not owned by the client: it is a **cache** of data that
lives on the server and can go stale. Putting it in a client store means re-implementing
loading flags, caching, deduplication, refetching and invalidation by hand. A query library
does that, and once server data lives there, the remaining "global" client state is often
small: the user, the theme, maybe a cart.

## When built-in tools stop scaling

`useState`, lifting state up and context + `useReducer` cover a lot. They start to hurt when:

- many components across the tree read **different slices** of a big, frequently changing
  object: every context consumer re-renders on every change;
- updates come from **outside React** (a WebSocket, a worker, a non-React widget);
- you want **devtools** with time travel, action logs, persistence, or middleware.

## External stores and selectors

A store keeps state **outside** React and lets each component subscribe to the slice it
needs, with a **selector**. The component re-renders only when its slice changes.

**Zustand** (small, hook-first):

```ts
import { create } from "zustand";

type CartStore = {
  items: Item[];
  add: (item: Item) => void;
};

export const useCartStore = create<CartStore>()((set) => ({
  items: [],
  add: (item) => set((state) => ({ items: [...state.items, item] })),
}));

function CartBadge() {
  const count = useCartStore((state) => state.items.length);  // re-renders only when the count changes
  return <span>{count}</span>;
}
```

**Redux Toolkit** (structured, with devtools and middleware) defines **slices** with reducers
written in a mutable style (Immer turns them into immutable updates), a `configureStore`, and
`useSelector` / `useDispatch` hooks. Its RTK Query module is a server-state cache too.

Other models exist: **Jotai** and Recoil-style atoms (state split into small independent
pieces), **MobX** (observable objects with automatic tracking), XState (explicit state
machines for complex flows).

## useSyncExternalStore

Every store connects to React through `useSyncExternalStore(subscribe, getSnapshot)`. It
subscribes to changes and reads a snapshot in a way that stays consistent during concurrent
rendering (no "tearing", where two components show different versions of the same store):

```ts
function useOnlineStatus() {
  return useSyncExternalStore(
    (callback) => {
      window.addEventListener("online", callback);
      window.addEventListener("offline", callback);
      return () => {
        window.removeEventListener("online", callback);
        window.removeEventListener("offline", callback);
      };
    },
    () => navigator.onLine,
  );
}
```

`getSnapshot` must return the same value (by `Object.is`) while nothing changed, or React
re-renders forever. You rarely write this yourself, but it explains how every store works.

## How to choose

1. Keep state **local** until two components need it; then **lift** it.
2. Put **server data** in a query cache (or the framework's loaders), not in your own store.
3. Put **shareable view state** in the URL.
4. Use **context** for rarely changing, widely needed values (user, theme, locale, services).
5. Add a **store** (Zustand for small to medium, Redux Toolkit for large teams that want
   strict structure and devtools) when shared client state is complex, updated often and read
   by many components.

Whatever you pick, keep state minimal and normalised (store ids and look things up, instead of
copies of the same object in several places), and keep update logic in pure, tested functions.

## Key points

- Classify state first: local, shared client, server, URL, form.
- Server state is a cache: use a query library, not a hand-made store.
- Context re-renders every consumer; stores with selectors re-render only what changed.
- `useSyncExternalStore` connects external stores to React safely under concurrency.
- Most apps need little global client state once server and URL state are in their place.

## Further reading

- react.dev, "Managing State": <https://react.dev/learn/managing-state>
- react.dev, `useSyncExternalStore`: <https://react.dev/reference/react/useSyncExternalStore>
- TanStack Query, "Does TanStack Query replace Redux, MobX or other global state managers?": <https://tanstack.com/query/latest/docs/framework/react/guides/does-this-replace-client-state>
- Redux Toolkit, "Getting Started": <https://redux-toolkit.js.org/introduction/getting-started>
- Zustand documentation: <https://zustand.docs.pmnd.rs/>
- Kent C. Dodds, "Application State Management with React": <https://kentcdodds.com/blog/application-state-management-with-react>
