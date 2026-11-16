package dojo.shop;

import dojo.shop.sales.Product;
import dojo.shop.sales.Products;
import dojo.shop.sales.SalesService;
import dojo.shop.warehouse.StockItem;
import dojo.shop.warehouse.StockItems;
import dojo.shop.warehouse.WarehouseService;
import java.util.List;

/** The composition root, and the only reader of the legacy format. */
public final class App {
    private App() {}

    public static Shop create(List<dojo.shop.catalog.Product> legacy) {
        Products products = new Products();
        StockItems items = new StockItems();
        for (dojo.shop.catalog.Product old : legacy) {
            products.save(new Product(old.sku(), old.name(), old.priceCents(), old.discontinued()));
            items.save(new StockItem(old.sku(), old.weightGrams(), old.binLocation(), old.stockOnHand()));
        }
        WarehouseService warehouse = new WarehouseService(items);
        return new Shop(new SalesService(products, warehouse), warehouse);
    }
}
