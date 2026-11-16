package dojo.shop.sales;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Sales' own store. */
public class Products {
    private final Map<String, Product> products = new LinkedHashMap<>();

    public void save(Product product) {
        products.put(product.sku(), product);
    }

    public Optional<Product> find(String sku) {
        return Optional.ofNullable(products.get(sku));
    }
}
