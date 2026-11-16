# Checkpoint 2: The Shop

Prices are integer **cents**.

## 1. The cart (`cart.ts`)

`cartReducer(state, action)` is pure and never mutates `state`:

| Action | Effect |
| --- | --- |
| `{ type: "add", product }` | a new line with quantity 1, or the existing line's quantity + 1 (lines keep their order) |
| `{ type: "remove", id }` | removes the line of that product |
| `{ type: "setQuantity", id, quantity }` | sets the quantity; below 1 removes the line |
| `{ type: "clear" }` | no lines |

When an action changes nothing (removing a product that is not in the cart), return **the
same state object**. Also write `cartCount(state)` (the sum of quantities), `cartTotal(state)`
(the total in cents) and `formatPrice(cents)` (`1250` → `"$12.50"`, `5` → `"$0.05"`).

## 2. The context (`CartContext.tsx`)

`<CartProvider initial={...}>` holds the reducer (initial state `{ lines: [] }` by default) and
provides `{ state, dispatch }`. `useCart()` returns it, and throws an `Error` with the message
`useCart must be used inside <CartProvider>` when there is no provider.

## 3. The components

**`ProductList`** (`ProductList.tsx`) gets `loadProducts(signal)`:

- while loading: `<p role="status">Loading products…</p>`;
- on failure: `<p role="alert">Could not load products: <message></p>` and a **Retry** button
  that loads again;
- when loaded: `<ul aria-label="Products">` with one `<li>` per product showing its name, its
  price (`formatPrice`) and a button whose accessible name is `Add <name> to cart`.

Abort the request when the component unmounts, and keep using the latest `loadProducts` the
component received without loading again on every render.

**`CartSummary`** (`CartSummary.tsx`) shows a heading **Cart** and either
`<p>Your cart is empty</p>` or a `<ul aria-label="Cart items">` with, per line, the product name,
a number input labelled `Quantity of <name>` (controlled by the line's quantity; changing it
dispatches `setQuantity`) and a button `Remove <name>`. Below: `<p>Total: $x.yy</p>` and a
**Clear cart** button (only when the cart is not empty).

**`CartBadge`** (`CartBadge.tsx`) shows `<p>Cart (<count>)</p>`.

**`Shop`** (`Shop.tsx`) puts `CartBadge`, `ProductList` and `CartSummary` inside one
`CartProvider`.

There are 13 tests: 1 in `tests/visible.test.tsx` and 12 hidden ones. Run them with
`npm install` once, then `npm test`.
