import { cartCount } from "./cart.ts";
import { useCart } from "./CartContext.tsx";

/** "Cart (<count>)". */
export function CartBadge() {
  const { state } = useCart();
  return <p>Cart ({cartCount(state)})</p>;
}
