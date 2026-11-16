package dojo.shop.application;

import dojo.shop.domain.Order;
import dojo.shop.domain.PriceConverter;
import dojo.shop.infra.OrderRow;
import dojo.shop.infra.OrderTable;

/** The use case: price an order in euros and store it. */
public class PlaceOrder {
    private final OrderTable table;
    private final PriceConverter converter;

    public PlaceOrder(OrderTable table, PriceConverter converter) {
        this.table = table;
        this.converter = converter;
    }

    /** Stores the order and returns its total in euro cents. */
    public long place(Order order) {
        long totalEuroCents = converter.toEuroCents(order.totalCents(), order.currency());
        table.insert(new OrderRow(order.id(), order.lines().size(), totalEuroCents));
        return totalEuroCents;
    }
}
