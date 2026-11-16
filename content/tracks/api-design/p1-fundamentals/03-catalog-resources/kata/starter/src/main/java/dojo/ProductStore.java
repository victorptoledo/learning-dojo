package dojo;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

/** The catalog, in memory. Starts with three products (ids 1 to 3). Written for you. */
@Component
public class ProductStore {
    private final Map<Long, Product> products = new ConcurrentSkipListMap<>();
    private final AtomicLong nextId = new AtomicLong(1);

    public ProductStore() {
        create(new ProductRequest("Kettle", "kitchen", 2999L));
        create(new ProductRequest("Toaster", "kitchen", 3499L));
        create(new ProductRequest("Desk lamp", "lighting", 1999L));
    }

    /** Every product, ordered by id. */
    public List<Product> all() {
        return List.copyOf(products.values());
    }

    /** The products of one category, ordered by id. */
    public List<Product> inCategory(String category) {
        return products.values().stream().filter(p -> p.category().equals(category)).toList();
    }

    public Optional<Product> find(long id) {
        return Optional.ofNullable(products.get(id));
    }

    public Product create(ProductRequest request) {
        long id = nextId.getAndIncrement();
        Product product = new Product(id, request.name(), request.category(), request.priceCents());
        products.put(id, product);
        return product;
    }

    /** Replaces every field of an existing product; empty if there is no such product. */
    public Optional<Product> replace(long id, ProductRequest request) {
        Product replaced = new Product(id, request.name(), request.category(), request.priceCents());
        return Optional.ofNullable(products.computeIfPresent(id, (key, old) -> replaced));
    }

    /** True if a product was removed. */
    public boolean delete(long id) {
        return products.remove(id) != null;
    }
}
