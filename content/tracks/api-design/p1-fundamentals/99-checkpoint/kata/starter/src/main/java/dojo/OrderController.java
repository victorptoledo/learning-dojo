package dojo;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The orders collection, one order, and the cancellation of an order. */
@RestController
@RequestMapping("/orders")
public class OrderController {
    private final OrderStore store;

    public OrderController(OrderStore store) {
        this.store = store;
    }
}
