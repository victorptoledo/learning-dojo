package dojo;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {
    private final ProductRepository products;
    private final OrderRepository orders;

    public OrderService(ProductRepository products, OrderRepository orders) {
        this.products = products;
        this.orders = orders;
    }

    /** Reserves the stock of every line and saves the order: all lines, or nothing. */
    @Transactional
    public OrderView place(OrderRequest request) {
        ShopOrder order = new ShopOrder();
        for (OrderRequest.Line line : request.lines()) {
            String sku = line.sku().strip();
            Product product = products.findBySku(sku).orElseThrow(() -> ShopException.unknownProduct(sku));
            product.reserve(line.quantity());
            order.addLine(sku, line.quantity(), product.getPriceCents());
        }
        return OrderView.of(orders.save(order));
    }

    @Transactional(readOnly = true)
    public OrderView get(long id) {
        return OrderView.of(orders.findWithLines(id).orElseThrow(() -> ShopException.orderNotFound(id)));
    }

    /** Cancels the order and puts its stock back. */
    @Transactional
    public OrderView cancel(long id) {
        ShopOrder order = orders.findWithLines(id).orElseThrow(() -> ShopException.orderNotFound(id));
        order.cancel();
        for (OrderLine line : order.getLines()) {
            products.findBySku(line.getSku()).ifPresent(product -> product.release(line.getQuantity()));
        }
        return OrderView.of(order);
    }
}
