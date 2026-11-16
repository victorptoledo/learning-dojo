import { useEffect, useRef, useState } from "react";
import { formatPrice, type Product } from "./cart.ts";
import { useCart } from "./CartContext.tsx";

export type ProductListProps = {
  loadProducts: (signal: AbortSignal) => Promise<readonly Product[]>;
};

type LoadState =
  | { status: "loading" }
  | { status: "error"; message: string }
  | { status: "ready"; products: readonly Product[] };

/** Loads the products and lists them with an Add button each. */
export function ProductList({ loadProducts }: ProductListProps) {
  const { dispatch } = useCart();
  const [state, setState] = useState<LoadState>({ status: "loading" });
  const [attempt, setAttempt] = useState(0);
  const loadRef = useRef(loadProducts);
  useEffect(() => {
    loadRef.current = loadProducts;
  });

  useEffect(() => {
    const controller = new AbortController();
    setState({ status: "loading" });
    loadRef.current(controller.signal).then(
      (products) => {
        if (!controller.signal.aborted) {
          setState({ status: "ready", products });
        }
      },
      (error: unknown) => {
        if (!controller.signal.aborted) {
          const message = error instanceof Error ? error.message : "unknown error";
          setState({ status: "error", message });
        }
      },
    );
    return () => controller.abort();
  }, [attempt]);

  if (state.status === "loading") {
    return <p role="status">Loading products…</p>;
  }
  if (state.status === "error") {
    return (
      <div>
        <p role="alert">Could not load products: {state.message}</p>
        <button type="button" onClick={() => setAttempt((n) => n + 1)}>
          Retry
        </button>
      </div>
    );
  }
  return (
    <ul aria-label="Products">
      {state.products.map((product) => (
        <li key={product.id}>
          {product.name} {formatPrice(product.price)}{" "}
          <button
            type="button"
            aria-label={`Add ${product.name} to cart`}
            onClick={() => dispatch({ type: "add", product })}
          >
            Add
          </button>
        </li>
      ))}
    </ul>
  );
}
