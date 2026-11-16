package dojo.shop;

import dojo.shop.application.PlaceOrder;
import dojo.shop.domain.PriceConverter;
import dojo.shop.infra.OrderTable;

/** The composition root: the one place that knows every package and wires them together. */
public final class App {
    private App() {}

    public static PlaceOrder placeOrder(OrderTable table) {
        return new PlaceOrder(table, new PriceConverter());
    }
}
