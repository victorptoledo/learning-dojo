import assert from "node:assert/strict";
import { describe, test } from "node:test";
import { act, fireEvent, render, renderHook, screen, within } from "@testing-library/react";
import { userEvent } from "@testing-library/user-event";
import { CartProvider, useCart } from "../CartContext.tsx";
import { CartSummary } from "../CartSummary.tsx";
import { ProductList } from "../ProductList.tsx";
import { Shop } from "../Shop.tsx";
import {
  cartCount,
  cartReducer,
  cartTotal,
  EMPTY_CART,
  formatPrice,
  type CartState,
  type Product,
} from "../cart.ts";

const tea: Product = Object.freeze({ id: "tea", name: "Tea", price: 450 });
const cake: Product = Object.freeze({ id: "cake", name: "Cake", price: 1250 });
const PRODUCTS = Object.freeze([tea, cake]);

function freezeCart(state: CartState): CartState {
  return Object.freeze({ lines: Object.freeze(state.lines.map((line) => Object.freeze(line))) });
}

const twoTeasOneCake = freezeCart({
  lines: [
    { product: tea, quantity: 2 },
    { product: cake, quantity: 1 },
  ],
});

function controlledLoad() {
  const calls: {
    signal: AbortSignal;
    resolve: (products: readonly Product[]) => void;
    reject: (error: unknown) => void;
  }[] = [];
  const load = (signal: AbortSignal) =>
    new Promise<readonly Product[]>((resolve, reject) => {
      calls.push({ signal, resolve, reject });
    });
  return { calls, load };
}

describe("cart", () => {
  test("add increments an existing line and keeps the order", () => {
    const state = cartReducer(twoTeasOneCake, { type: "add", product: tea });
    assert.deepEqual(
      state.lines.map((line) => [line.product.id, line.quantity]),
      [
        ["tea", 3],
        ["cake", 1],
      ],
    );
    assert.equal(twoTeasOneCake.lines[0]?.quantity, 2);
  });

  test("remove, setQuantity and clear", () => {
    assert.deepEqual(
      cartReducer(twoTeasOneCake, { type: "remove", id: "tea" }).lines.map((l) => l.product.id),
      ["cake"],
    );
    const five = cartReducer(twoTeasOneCake, { type: "setQuantity", id: "cake", quantity: 5 });
    assert.equal(five.lines[1]?.quantity, 5);
    const zero = cartReducer(twoTeasOneCake, { type: "setQuantity", id: "tea", quantity: 0 });
    assert.deepEqual(
      zero.lines.map((l) => l.product.id),
      ["cake"],
    );
    assert.deepEqual(cartReducer(twoTeasOneCake, { type: "clear" }), { lines: [] });
  });

  test("an action that changes nothing returns the same state", () => {
    assert.equal(cartReducer(twoTeasOneCake, { type: "remove", id: "coffee" }), twoTeasOneCake);
    assert.equal(cartReducer(EMPTY_CART, { type: "clear" }).lines.length, 0);
  });

  test("count, total and price formatting", () => {
    assert.equal(cartCount(twoTeasOneCake), 3);
    assert.equal(cartTotal(twoTeasOneCake), 2150);
    assert.equal(formatPrice(1250), "$12.50");
    assert.equal(formatPrice(5), "$0.05");
    assert.equal(formatPrice(0), "$0.00");
  });
});

describe("CartContext", () => {
  test("useCart throws a clear error outside the provider", () => {
    const original = console.error;
    console.error = () => {};
    try {
      assert.throws(() => renderHook(() => useCart()), {
        message: "useCart must be used inside <CartProvider>",
      });
    } finally {
      console.error = original;
    }
  });

  test("useCart reads and changes the cart inside the provider", () => {
    const { result } = renderHook(() => useCart(), {
      wrapper: ({ children }) => <CartProvider>{children}</CartProvider>,
    });
    assert.deepEqual(result.current.state, { lines: [] });
    act(() => result.current.dispatch({ type: "add", product: cake }));
    assert.equal(cartCount(result.current.state), 1);
  });
});

describe("ProductList", () => {
  test("shows a status while loading, then the products", async () => {
    const { calls, load } = controlledLoad();
    render(
      <CartProvider>
        <ProductList loadProducts={load} />
      </CartProvider>,
    );
    assert.equal(screen.getByRole("status").textContent, "Loading products…");
    await act(async () => calls[0]?.resolve(PRODUCTS));
    const list = screen.getByRole("list", { name: "Products" });
    assert.equal(within(list).getAllByRole("listitem").length, 2);
    assert.ok(within(list).getByText("$12.50", { exact: false }));
    assert.ok(screen.getByRole("button", { name: "Add Cake to cart" }));
    assert.equal(screen.queryByRole("status"), null);
  });

  test("shows the error with a Retry button that loads again", async () => {
    const user = userEvent.setup();
    const { calls, load } = controlledLoad();
    render(
      <CartProvider>
        <ProductList loadProducts={load} />
      </CartProvider>,
    );
    await act(async () => calls[0]?.reject(new Error("HTTP 503")));
    assert.equal(screen.getByRole("alert").textContent, "Could not load products: HTTP 503");
    await user.click(screen.getByRole("button", { name: "Retry" }));
    assert.equal(calls.length, 2);
    assert.ok(screen.getByRole("status"));
    await act(async () => calls[1]?.resolve([tea]));
    assert.ok(screen.getByRole("button", { name: "Add Tea to cart" }));
  });

  test("aborts on unmount and does not reload when the parent re-renders", async () => {
    const { calls, load } = controlledLoad();
    const { rerender, unmount } = render(
      <CartProvider>
        <ProductList loadProducts={(signal) => load(signal)} />
      </CartProvider>,
    );
    rerender(
      <CartProvider>
        <ProductList loadProducts={(signal) => load(signal)} />
      </CartProvider>,
    );
    assert.equal(calls.length, 1);
    unmount();
    assert.equal(calls[0]?.signal.aborted, true);
  });
});

describe("CartSummary and Shop", () => {
  test("CartSummary edits quantities, removes lines and shows the total", async () => {
    const user = userEvent.setup();
    render(
      <CartProvider initial={twoTeasOneCake}>
        <CartSummary />
      </CartProvider>,
    );
    assert.ok(screen.getByRole("heading", { name: "Cart" }));
    assert.ok(screen.getByText("Total: $21.50"));
    const quantity = screen.getByLabelText("Quantity of Cake") as HTMLInputElement;
    assert.equal(quantity.value, "1");
    fireEvent.change(quantity, { target: { value: "3" } });
    assert.ok(screen.getByText("Total: $46.50"));
    await user.click(screen.getByRole("button", { name: "Remove Tea" }));
    assert.equal(screen.queryByLabelText("Quantity of Tea"), null);
    assert.ok(screen.getByText("Total: $37.50"));
  });

  test("CartSummary says when the cart is empty", async () => {
    const user = userEvent.setup();
    render(
      <CartProvider initial={twoTeasOneCake}>
        <CartSummary />
      </CartProvider>,
    );
    await user.click(screen.getByRole("button", { name: "Clear cart" }));
    assert.ok(screen.getByText("Your cart is empty"));
    assert.equal(screen.queryByRole("button", { name: "Clear cart" }), null);
    assert.equal(screen.queryByRole("list", { name: "Cart items" }), null);
  });

  test("Shop wires the list, the badge and the summary to one cart", async () => {
    const user = userEvent.setup();
    render(<Shop loadProducts={async () => PRODUCTS} />);
    const add = await screen.findByRole("button", { name: "Add Tea to cart" });
    assert.ok(screen.getByText("Cart (0)"));
    await user.click(add);
    await user.click(add);
    await user.click(screen.getByRole("button", { name: "Add Cake to cart" }));
    assert.ok(screen.getByText("Cart (3)"));
    const items = within(screen.getByRole("list", { name: "Cart items" }));
    assert.equal((items.getByLabelText("Quantity of Tea") as HTMLInputElement).value, "2");
    assert.ok(screen.getByText("Total: $21.50"));
  });
});
