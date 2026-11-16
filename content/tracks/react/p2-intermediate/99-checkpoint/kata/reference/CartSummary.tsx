import { cartTotal, formatPrice } from "./cart.ts";
import { useCart } from "./CartContext.tsx";

/** The lines of the cart with quantities, Remove buttons, the total and Clear cart. */
export function CartSummary() {
  const { state, dispatch } = useCart();
  return (
    <section>
      <h2>Cart</h2>
      {state.lines.length === 0 ? (
        <p>Your cart is empty</p>
      ) : (
        <>
          <ul aria-label="Cart items">
            {state.lines.map(({ product, quantity }) => (
              <li key={product.id}>
                {product.name}{" "}
                <label>
                  Quantity of {product.name}
                  <input
                    type="number"
                    min={0}
                    value={quantity}
                    onChange={(event) =>
                      dispatch({
                        type: "setQuantity",
                        id: product.id,
                        quantity: Number(event.target.value),
                      })
                    }
                  />
                </label>
                <button type="button" onClick={() => dispatch({ type: "remove", id: product.id })}>
                  Remove {product.name}
                </button>
              </li>
            ))}
          </ul>
          <button type="button" onClick={() => dispatch({ type: "clear" })}>
            Clear cart
          </button>
        </>
      )}
      <p>Total: {formatPrice(cartTotal(state))}</p>
    </section>
  );
}
