package dojo;

import java.util.List;

public record OrderView(long id, OrderStatus status, long totalCents, List<LineView> lines) {
    public record LineView(String sku, int quantity, long unitCents) {}

    public static OrderView of(ShopOrder order) {
        return new OrderView(order.getId(), order.getStatus(), order.totalCents(),
                order.getLines().stream()
                        .map(line -> new LineView(line.getSku(), line.getQuantity(), line.getUnitCents()))
                        .toList());
    }
}
