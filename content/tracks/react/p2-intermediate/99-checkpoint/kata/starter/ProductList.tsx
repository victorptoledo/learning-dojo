import type { ReactNode } from "react";
import type { Product } from "./cart.ts";

export type ProductListProps = {
  loadProducts: (signal: AbortSignal) => Promise<readonly Product[]>;
};

/** Loads the products and lists them with an Add button each. */
export function ProductList(_props: ProductListProps): ReactNode {
  throw new Error("todo: write ProductList");
}
