package dojo.shop.catalog;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** The shared store both services write to. */
public class ProductRepository {
    private final Map<String, Product> products = new LinkedHashMap<>();

    public void save(Product product) {
        products.put(product.sku(), product);
    }

    public Optional<Product> find(String sku) {
        return Optional.ofNullable(products.get(sku));
    }
}
