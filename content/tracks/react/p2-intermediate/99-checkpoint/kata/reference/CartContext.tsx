import { createContext, useContext, useMemo, useReducer, type Dispatch, type ReactNode } from "react";
import { cartReducer, EMPTY_CART, type CartAction, type CartState } from "./cart.ts";

export type CartContextValue = {
  state: CartState;
  dispatch: Dispatch<CartAction>;
};

export type CartProviderProps = {
  initial?: CartState;
  children: ReactNode;
};

const CartContext = createContext<CartContextValue | null>(null);

/** Holds the cart reducer and provides { state, dispatch }. */
export function CartProvider({ initial = EMPTY_CART, children }: CartProviderProps) {
  const [state, dispatch] = useReducer(cartReducer, initial);
  const value = useMemo(() => ({ state, dispatch }), [state]);
  return <CartContext value={value}>{children}</CartContext>;
}

/** The cart of the nearest CartProvider; throws outside one. */
export function useCart(): CartContextValue {
  const cart = useContext(CartContext);
  if (cart === null) {
    throw new Error("useCart must be used inside <CartProvider>");
  }
  return cart;
}
