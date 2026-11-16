import { CartBadge } from "./CartBadge.tsx";
import { CartProvider } from "./CartContext.tsx";
import { CartSummary } from "./CartSummary.tsx";
import { ProductList, type ProductListProps } from "./ProductList.tsx";

/** CartBadge, ProductList and CartSummary inside one CartProvider. */
export function Shop({ loadProducts }: ProductListProps) {
  return (
    <CartProvider>
      <CartBadge />
      <ProductList loadProducts={loadProducts} />
      <CartSummary />
    </CartProvider>
  );
}
