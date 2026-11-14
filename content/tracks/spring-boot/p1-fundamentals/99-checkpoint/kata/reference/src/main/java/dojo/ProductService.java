package dojo;

import java.util.List;
import java.util.NavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import org.springframework.stereotype.Service;

/** The catalog, in memory, ordered by sku. */
@Service
public class ProductService {
    private final NavigableMap<String, Product> products = new ConcurrentSkipListMap<>();
    private final CatalogProperties catalog;

    public ProductService(CatalogProperties catalog) {
        this.catalog = catalog;
    }

    public Product add(ProductRequest request) {
        Product product = new Product(request.sku(), request.name().strip(), request.priceCents(),
                request.category().strip().toLowerCase());
        if (products.putIfAbsent(product.sku(), product) != null) {
            throw CatalogException.duplicate(product.sku());
        }
        return product;
    }

    public Product get(String sku) {
        Product product = products.get(sku);
        if (product == null) {
            throw CatalogException.notFound(sku);
        }
        return product;
    }

    /** Products of {@code category} (all when null), sorted by sku, one page of {@code size}. */
    public ProductPage list(String category, int page, int size) {
        if (size > catalog.maxPageSize()) {
            throw CatalogException.pageTooLarge(size, catalog.maxPageSize());
        }
        List<Product> matching = products.values().stream()
                .filter(p -> category == null || p.category().equals(category.strip().toLowerCase()))
                .toList();
        List<Product> items = matching.stream().skip((long) page * size).limit(size).toList();
        return new ProductPage(items, page, size, matching.size(), catalog.currency());
    }

    public PriceQuote discount(String sku, int percent) {
        Product product = get(sku);
        if (percent > catalog.maxDiscountPercent()) {
            throw CatalogException.discountTooHigh(percent, catalog.maxDiscountPercent());
        }
        long discounted = product.priceCents() * (100 - percent) / 100;
        return new PriceQuote(sku, product.priceCents(), discounted, catalog.currency());
    }
}
