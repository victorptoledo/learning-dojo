package dojo.store;

import dojo.store.catalog.api.Catalog;
import dojo.store.catalog.internal.CatalogService;
import dojo.store.catalog.internal.ProductRow;
import dojo.store.catalog.internal.ProductTable;
import dojo.store.orders.internal.OrderService;
import dojo.store.shipping.api.Shipping;
import dojo.store.shipping.internal.ShippingService;
import java.util.List;

/** The composition root: it may know every module's internals, nothing else may. */
public final class App {
    private App() {}

    public static Store create(List<ProductRow> products) {
        Catalog catalog = new CatalogService(new ProductTable(products));
        Shipping shipping = new ShippingService();
        return new Store(catalog, new OrderService(catalog, shipping), shipping);
    }
}
