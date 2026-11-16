import type { Dispatch, ReactNode } from "react";
import type { CartAction, CartState } from "./cart.ts";

export type CartContextValue = {
  state: CartState;
  dispatch: Dispatch<CartAction>;
};

export type CartProviderProps = {
  initial?: CartState;
  children: ReactNode;
};

/** Holds the cart reducer and provides { state, dispatch }. */
export function CartProvider(_props: CartProviderProps): ReactNode {
  throw new Error("todo: write CartProvider");
}

/** The cart of the nearest CartProvider; throws outside one. */
export function useCart(): CartContextValue {
  throw new Error("todo: write useCart");
}
