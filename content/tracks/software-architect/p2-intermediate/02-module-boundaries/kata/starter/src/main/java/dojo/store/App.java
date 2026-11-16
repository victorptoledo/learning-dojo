package dojo.store;

import dojo.store.catalog.internal.CatalogService;
import dojo.store.catalog.internal.ProductRow;
import dojo.store.catalog.internal.ProductTable;
import dojo.store.orders.internal.OrderService;
import dojo.store.orders.internal.OrderStore;
import dojo.store.shipping.internal.ShippingService;
import java.util.List;

/** The composition root: it may know every module's internals, nothing else may. */
public final class App {
    private App() {}

    public static Store create(List<ProductRow> products) {
        ProductTable table = new ProductTable(products);
        OrderStore orderStore = new OrderStore();
        ShippingService shipping = new ShippingService(orderStore);
        return new Store(new CatalogService(table), new OrderService(table, orderStore, shipping), shipping);
    }
}
