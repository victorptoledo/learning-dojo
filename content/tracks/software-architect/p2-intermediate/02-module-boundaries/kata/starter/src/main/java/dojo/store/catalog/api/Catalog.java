package dojo.store.catalog.api;

import java.util.Optional;

public interface Catalog {
    Optional<ProductView> product(String sku);
}
