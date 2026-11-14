package dojo;

import org.springframework.stereotype.Service;

/** The catalog, in memory, ordered by sku. */
@Service
public class ProductService {

    public Product add(ProductRequest request) {
        throw new UnsupportedOperationException("todo add " + request);
    }

    public Product get(String sku) {
        throw new UnsupportedOperationException("todo get " + sku);
    }

    /** Products of {@code category} (all when null), sorted by sku, one page of {@code size}. */
    public ProductPage list(String category, int page, int size) {
        throw new UnsupportedOperationException("todo list " + category);
    }

    public PriceQuote discount(String sku, int percent) {
        throw new UnsupportedOperationException("todo discount " + sku);
    }
}
