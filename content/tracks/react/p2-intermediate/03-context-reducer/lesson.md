# Context and useReducer

Props are explicit, which is good, until a value is needed by many components at different
depths. Passing it through every layer in between is called **prop drilling**. **Context**
lets a parent provide a value to its whole subtree. **`useReducer`** moves complex state
updates into one pure function. Together they are React's built-in answer to "app state".

## Context

```tsx
import { createContext, useContext, useState, type ReactNode } from "react";

type Theme = "light" | "dark";
const ThemeContext = createContext<Theme>("light");      // default when there is no provider

export function App() {
  const [theme, setTheme] = useState<Theme>("dark");
  return (
    <ThemeContext value={theme}>                           {/* React 19: the context is the provider */}
      <Page />
    </ThemeContext>
  );
}

function Button({ children }: { children: ReactNode }) {
  const theme = useContext(ThemeContext);                  // nearest provider above, or the default
  return <button className={`btn btn-${theme}`}>{children}</button>;
}
```

In React 19 you render `<ThemeContext value={...}>` directly; older code writes
`<ThemeContext.Provider value={...}>`, which still works. A component reads the value of the
**nearest** provider above it; providers can be nested to override a value for a subtree.

### A typed context with a guard hook

For values that have no sensible default (the current user, a cart), make the default `null`
and wrap `useContext` in a hook that fails clearly:

```tsx
type Session = { user: User; logout: () => void };
const SessionContext = createContext<Session | null>(null);

export function useSession(): Session {
  const session = useContext(SessionContext);
  if (session === null) {
    throw new Error("useSession must be used inside <SessionProvider>");
  }
  return session;
}
```

Callers get a non-null `Session` type and a readable error if they forget the provider.

### What context is good for

Values that many components need and that change rarely: the theme, the signed-in user, the
locale, a router, a feature-flag client, a design-system configuration. Context is a
**dependency-injection** mechanism, not a state-management library: it does not decide how
state changes, it only delivers a value.

### The cost: re-renders

When a provider's `value` changes (by `Object.is`), **every** component that reads that
context re-renders, even if it uses a part that did not change. An object literal as value,
`value={{ user, logout }}`, is a new object on every render of the provider. Remedies:

- memoise the value with `useMemo` (performance lesson);
- **split** contexts: one for data that changes often, one for stable functions (`dispatch`
  never changes), or one per concern;
- keep fast-changing state (the text of an input) local, not in context.

## useReducer

When state has several fields that change together, or the next state depends on the
previous one in many ways, put the logic in a **reducer**: a pure function
`(state, action) => newState`.

```tsx
type Item = { id: string; name: string; price: number; quantity: number };
type CartState = { items: readonly Item[] };
type CartAction =
  | { type: "add"; item: Omit<Item, "quantity"> }
  | { type: "remove"; id: string }
  | { type: "clear" };

export function cartReducer(state: CartState, action: CartAction): CartState {
  switch (action.type) {
    case "add": {
      const existing = state.items.find((i) => i.id === action.item.id);
      return existing
        ? { items: state.items.map((i) => (i.id === action.item.id ? { ...i, quantity: i.quantity + 1 } : i)) }
        : { items: [...state.items, { ...action.item, quantity: 1 }] };
    }
    case "remove":
      return { items: state.items.filter((i) => i.id !== action.id) };
    case "clear":
      return { items: [] };
  }
}

function Cart() {
  const [state, dispatch] = useReducer(cartReducer, { items: [] });
  return <button onClick={() => dispatch({ type: "clear" })}>Empty cart</button>;
}
```

Benefits: the components say **what happened** (`dispatch({ type: "add", item })`) and the
reducer decides **how state changes**, in one place you can test without React. The actions
are a discriminated union, so TypeScript checks every `case`. The rules are those of state:
never mutate, always return a new object (or the same one when nothing changes).

`useState` or `useReducer`? Start with `useState`. Switch to a reducer when several setters
must change together, when updates are scattered across many handlers, or when you want the
transitions written down and tested.

## Reducer + context

Provide the state and `dispatch` through context and wrap them in hooks:

```tsx
const CartContext = createContext<{ state: CartState; dispatch: Dispatch<CartAction> } | null>(null);

export function CartProvider({ children }: { children: ReactNode }) {
  const [state, dispatch] = useReducer(cartReducer, { items: [] });
  const value = useMemo(() => ({ state, dispatch }), [state]);
  return <CartContext value={value}>{children}</CartContext>;
}

export function useCart() {
  const cart = useContext(CartContext);
  if (cart === null) throw new Error("useCart must be used inside <CartProvider>");
  return cart;
}
```

Any component under `CartProvider` can read the cart or dispatch actions, with no prop
drilling. For larger apps with frequent updates and many consumers, a store library (next
phase) gives finer-grained subscriptions.

## Key points

- Context delivers a value to a subtree; read it with `useContext` (or `use`).
- Type it as `T | null` and expose a guard hook that throws outside the provider.
- Every consumer re-renders when the value changes: memoise, split, keep fast state local.
- A reducer is a pure `(state, action) => state` with typed actions; test it without React.
- Reducer + context is the built-in pattern for shared app state.

## Further reading

- react.dev, "Passing Data Deeply with Context": <https://react.dev/learn/passing-data-deeply-with-context>
- react.dev, "Extracting State Logic into a Reducer": <https://react.dev/learn/extracting-state-logic-into-a-reducer>
- react.dev, "Scaling Up with Reducer and Context": <https://react.dev/learn/scaling-up-with-reducer-and-context>
- react.dev, `useContext`: <https://react.dev/reference/react/useContext>
- react.dev, `useReducer`: <https://react.dev/reference/react/useReducer>
- React 19 release notes, `<Context>` as a provider: <https://react.dev/blog/2024/12/05/react-19>
