export type Product = {
  readonly id: string;
  readonly name: string;
  /** In cents. */
  readonly price: number;
};

export type CartLine = {
  readonly product: Product;
  readonly quantity: number;
};

export type CartState = {
  readonly lines: readonly CartLine[];
};

export type CartAction =
  | { type: "add"; product: Product }
  | { type: "remove"; id: string }
  | { type: "setQuantity"; id: string; quantity: number }
  | { type: "clear" };

export const EMPTY_CART: CartState = { lines: [] };

/** The next cart; the same object when the action changes nothing. */
export function cartReducer(_state: CartState, _action: CartAction): CartState {
  throw new Error("todo: write cartReducer");
}

/** The number of items: the sum of the quantities. */
export function cartCount(_state: CartState): number {
  throw new Error("todo: write cartCount");
}

/** The total price in cents. */
export function cartTotal(_state: CartState): number {
  throw new Error("todo: write cartTotal");
}

/** 1250 → "$12.50". */
export function formatPrice(_cents: number): string {
  throw new Error("todo: write formatPrice");
}
