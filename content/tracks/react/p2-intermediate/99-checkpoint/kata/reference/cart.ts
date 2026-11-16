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

function has(state: CartState, id: string): boolean {
  return state.lines.some((line) => line.product.id === id);
}

/** The next cart; the same object when the action changes nothing. */
export function cartReducer(state: CartState, action: CartAction): CartState {
  switch (action.type) {
    case "add": {
      const { product } = action;
      if (!has(state, product.id)) {
        return { lines: [...state.lines, { product, quantity: 1 }] };
      }
      return {
        lines: state.lines.map((line) =>
          line.product.id === product.id ? { ...line, quantity: line.quantity + 1 } : line,
        ),
      };
    }
    case "remove":
      if (!has(state, action.id)) {
        return state;
      }
      return { lines: state.lines.filter((line) => line.product.id !== action.id) };
    case "setQuantity":
      if (action.quantity < 1) {
        return cartReducer(state, { type: "remove", id: action.id });
      }
      if (!has(state, action.id)) {
        return state;
      }
      return {
        lines: state.lines.map((line) =>
          line.product.id === action.id ? { ...line, quantity: action.quantity } : line,
        ),
      };
    case "clear":
      return state.lines.length === 0 ? state : { lines: [] };
  }
}

/** The number of items: the sum of the quantities. */
export function cartCount(state: CartState): number {
  return state.lines.reduce((sum, line) => sum + line.quantity, 0);
}

/** The total price in cents. */
export function cartTotal(state: CartState): number {
  return state.lines.reduce((sum, line) => sum + line.quantity * line.product.price, 0);
}

/** 1250 → "$12.50". */
export function formatPrice(cents: number): string {
  return `$${(cents / 100).toFixed(2)}`;
}
