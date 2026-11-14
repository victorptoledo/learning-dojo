package dojo;

import org.springframework.stereotype.Service;

@Service
public class OrderService {
    private final ProductRepository products;
    private final OrderRepository orders;

    public OrderService(ProductRepository products, OrderRepository orders) {
        this.products = products;
        this.orders = orders;
    }

    /** Reserves the stock of every line and saves the order: all lines, or nothing. */
    public OrderView place(OrderRequest request) {
        throw new UnsupportedOperationException("todo place " + request);
    }

    public OrderView get(long id) {
        throw new UnsupportedOperationException("todo get " + id);
    }

    /** Cancels the order and puts its stock back. */
    public OrderView cancel(long id) {
        throw new UnsupportedOperationException("todo cancel " + id);
    }
}
