package dojo.shop;

import dojo.shop.catalog.Product;
import dojo.shop.catalog.ProductRepository;
import dojo.shop.sales.SalesService;
import dojo.shop.warehouse.WarehouseService;
import java.util.List;

/** The composition root. */
public final class App {
    private App() {}

    public static Shop create(List<Product> legacy) {
        ProductRepository repository = new ProductRepository();
        legacy.forEach(repository::save);
        return new Shop(new SalesService(repository), new WarehouseService(repository));
    }
}
